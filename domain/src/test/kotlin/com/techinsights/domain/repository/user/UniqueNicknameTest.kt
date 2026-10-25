package com.techinsights.domain.repository.user

import com.techinsights.domain.exception.user.DuplicateNicknameException
import com.techinsights.domain.service.user.UserService
import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.support.SqlFixtures
import com.techinsights.domain.validator.NicknameValidator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@Import(UserService::class, NicknameValidator::class)
class UniqueNicknameTest(
  private val userService: UserService,
  private val userRepository: UserRepository,
  private val jdbc: JdbcTemplate,
  private val transactionTemplate: TransactionTemplate
) : RepositoryTestSpec({

  val fixtures = SqlFixtures(jdbc)
  val at = { minute: Int -> LocalDateTime.of(2024, 1, 1, 0, minute) }

  fun nickname(id: Long) = jdbc.queryForObject("SELECT nickname FROM users WHERE id = ?", String::class.java, id)

  context("V029 de-duplication") {
    fun runMigration() {
      jdbc.execute("DROP INDEX IF EXISTS uk_users_nickname_lower")
      // seeding duplicates is only possible without the index; the script recreates it
    }

    fun script() = ClassPathResource("db/migration/V029__Unique_Nickname_Case_Insensitive.sql")
      .inputStream.bufferedReader().readText()

    test("keeps the earliest holder per lower(nickname) and renames the rest, ignoring deleted users and nulls") {
      runMigration()
      fixtures.insertUser(1L, "Foo", at(5))
      fixtures.insertUser(2L, "foo", at(1)) // earliest -> keeps
      fixtures.insertUser(3L, "FOO", at(1)) // tie on created_at -> larger id loses
      fixtures.insertUser(4L, "bar", at(1))
      fixtures.insertUser(5L, "Bar", at(2), deleted = true) // deleted: untouched
      fixtures.insertUser(6L, null, at(1))
      fixtures.insertUser(7L, null, at(2))

      jdbc.execute(script())

      nickname(2L) shouldBe "foo"
      nickname(1L) shouldBe "User_1"
      nickname(3L) shouldBe "User_3"
      listOf(1L, 3L).forEach { (nickname(it).length <= 20) shouldBe true }
      nickname(4L) shouldBe "bar"
      nickname(5L) shouldBe "Bar"
    }

    test("renamed losers stay within the nickname length limit, unique and collision-free") {
      runMigration()
      // two ids sharing the same last 12 digits, plus an active user already named like the first candidate
      fixtures.insertUser(1000000000000000001L, "dup", at(1))
      fixtures.insertUser(2000000000000000001L, "DUP", at(2))
      fixtures.insertUser(3000000000000000001L, "Dup", at(3))
      fixtures.insertUser(9L, "User_000000000001", at(1))

      jdbc.execute(script())

      val names = jdbc.queryForList("SELECT nickname FROM users", String::class.java)
      names.forEach { (it.length <= 20) shouldBe true }
      names.map { it.lowercase() }.toSet().size shouldBe names.size
      nickname(1000000000000000001L) shouldBe "dup"
      nickname(9L) shouldBe "User_000000000001"
      nickname(2000000000000000001L).startsWith("User_") shouldBe true
      nickname(3000000000000000001L).startsWith("User_") shouldBe true
    }

    test("an existing case-sensitive unique constraint or index on nickname is dropped, others are kept") {
      runMigration()
      jdbc.execute("ALTER TABLE users ADD CONSTRAINT uk_hibernate_nickname UNIQUE (nickname)")
      jdbc.execute("CREATE UNIQUE INDEX uq_nickname_plain ON users (nickname)")
      fixtures.insertUser(1L, "Taken", at(1), deleted = true)

      jdbc.execute(script())

      fun exists(sql: String) = jdbc.queryForObject(sql, Int::class.java)!! > 0
      exists("SELECT count(*) FROM pg_constraint WHERE conname = 'uk_hibernate_nickname'") shouldBe false
      exists("SELECT count(*) FROM pg_class WHERE relname = 'uq_nickname_plain'") shouldBe false
      exists("SELECT count(*) FROM pg_constraint WHERE conname = 'uk_users_provider_provider_id'") shouldBe true
      exists("SELECT count(*) FROM pg_class WHERE relname = 'uk_users_nickname_lower'") shouldBe true
      fixtures.insertUser(2L, "Taken", at(2)) // would have violated the old constraint
      nickname(2L) shouldBe "Taken"
    }

    test("the created index rejects case variants but lets a deleted user's nickname be reused") {
      runMigration()
      jdbc.execute(script())
      fixtures.insertUser(1L, "Taken", at(1), deleted = true)
      fixtures.insertUser(2L, "taken", at(2)) // reuse of a deleted holder's nickname is fine

      shouldThrow<Exception> { fixtures.insertUser(3L, "TAKEN", at(3)) }
      nickname(2L) shouldBe "taken"
    }
  }

  context("updateNickname") {
    beforeEach {
      fixtures.insertUser(1L, "alice1", at(1))
      fixtures.insertUser(2L, "bob22", at(2))
    }

    test("a case variant of someone else's nickname is rejected") {
      shouldThrow<DuplicateNicknameException> { userService.updateNickname(2L, "ALICE1") }
      nickname(2L) shouldBe "bob22"
    }

    test("changing only the case of one's own nickname is allowed") {
      userService.updateNickname(1L, "Alice1")

      nickname(1L) shouldBe "Alice1"
    }

    test("a deleted user's nickname can be taken") {
      fixtures.insertUser(3L, "ghost33", at(3), deleted = true)

      userService.updateNickname(2L, "ghost33")

      nickname(2L) shouldBe "ghost33"
    }

    test("concurrent updates to the same nickname: exactly one wins, the other gets a duplicate error") {
      fixtures.insertUser(3L, "carol3", at(3))
      val executor = Executors.newFixedThreadPool(2)
      val start = CountDownLatch(1)
      val results = listOf(2L, 3L).map { id ->
        executor.submit<Result<Unit>> {
          start.await()
          runCatching { userService.updateNickname(id, "Contested") }.map { }
        }
      }
      start.countDown()
      val outcomes = results.map { it.get() }
      executor.shutdown()

      outcomes.count { it.isSuccess } shouldBe 1
      outcomes.single { it.isFailure }.exceptionOrNull().shouldBeDuplicate()
      jdbc.queryForObject("SELECT count(*) FROM users WHERE lower(nickname) = 'contested'", Int::class.java) shouldBe 1
    }

    test("a violation that slips past the pre-check surfaces as DuplicateNicknameException from the repository") {
      // user 2 holds the nickname in a transaction that stays open while user 1 tries to take it
      val firstHolds = CountDownLatch(1)
      val executor = Executors.newSingleThreadExecutor()
      val holder = executor.submit {
        transactionTemplate.execute {
          val bob = userRepository.findById(2L).get()
          bob.nickname = "Racer77"
          userRepository.saveNickname(bob)
          firstHolds.countDown()
          Thread.sleep(500)
        }
      }
      firstHolds.await()

      val alice = userRepository.findById(1L).get().also { it.nickname = "racer77" }
      shouldThrow<DuplicateNicknameException> { transactionTemplate.execute { userRepository.saveNickname(alice) } }

      holder.get()
      executor.shutdown()
      nickname(2L) shouldBe "Racer77"
      nickname(1L) shouldBe "alice1"
    }
  }
})

private fun Throwable?.shouldBeDuplicate() {
  (this is DuplicateNicknameException) shouldBe true
}
