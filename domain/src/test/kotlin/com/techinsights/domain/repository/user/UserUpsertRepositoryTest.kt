package com.techinsights.domain.repository.user

import com.techinsights.domain.enums.ProviderType
import com.techinsights.domain.support.RepositoryTestSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

class UserUpsertRepositoryTest(
  private val repository: UserRepository,
  transactionManager: PlatformTransactionManager,
  jdbcTemplate: JdbcTemplate
) : RepositoryTestSpec({
  val tx = TransactionTemplate(transactionManager)

  fun insertUser(id: Long, providerId: String, status: String = "ACTIVE", deleted: Boolean = false) {
    jdbcTemplate.update(
      """
      INSERT INTO users (id, email, name, nickname, provider, provider_id, status, deleted_at, last_login_at)
      VALUES (?, ?, 'Old Name', ?, 'GOOGLE', ?, ?, ${if (deleted) "now() - interval '1 day'" else "NULL"}, NULL)
      """.trimIndent(),
      id, "old$id@example.com", "nick$id", providerId, status
    )
  }

  fun upsert(providerId: String) = tx.execute {
    repository.upsertByProvider(
      id = 999L,
      email = "new@example.com",
      name = "New Name",
      nickname = "User_new",
      provider = ProviderType.GOOGLE,
      providerId = providerId,
      profileImage = "new.png"
    )
  }

  fun row(providerId: String): Map<String, Any?> =
    jdbcTemplate.queryForMap(
      "SELECT email, name, profile_image, last_login_at, deleted_at, status, updated_at FROM users WHERE provider_id = ?",
      providerId
    )

  test("활성 사용자는 upsert 로 email/name/profileImage/lastLoginAt 이 갱신된다") {
    insertUser(1, "g-1")

    upsert("g-1")

    val row = row("g-1")
    row["email"] shouldBe "new@example.com"
    row["name"] shouldBe "New Name"
    row["profile_image"] shouldBe "new.png"
    (row["last_login_at"] != null) shouldBe true
    // the existing row keeps its identity: no new row, original id and nickname
    jdbcTemplate.queryForObject("SELECT count(*) FROM users", Int::class.java) shouldBe 1
    jdbcTemplate.queryForObject("SELECT id FROM users WHERE provider_id = 'g-1'", Long::class.java) shouldBe 1L
    jdbcTemplate.queryForObject("SELECT nickname FROM users WHERE provider_id = 'g-1'", String::class.java) shouldBe "nick1"
  }

  test("soft-delete 된 사용자의 행은 upsert 로 변경되지 않는다") {
    insertUser(1, "g-1", deleted = true)
    val before = row("g-1")

    upsert("g-1")

    row("g-1") shouldBe before
    jdbcTemplate.queryForObject("SELECT count(*) FROM users", Int::class.java) shouldBe 1
  }

  test("ACTIVE 가 아닌 사용자의 행은 upsert 로 변경되지 않는다") {
    insertUser(1, "g-1", status = "INACTIVE")
    insertUser(2, "g-2", status = "BANNED")
    val before1 = row("g-1")
    val before2 = row("g-2")

    upsert("g-1")
    upsert("g-2")

    row("g-1") shouldBe before1
    row("g-2") shouldBe before2
  }

  test("없는 계정은 upsert 로 새로 만들어진다") {
    upsert("g-new")

    row("g-new")["email"] shouldBe "new@example.com"
    jdbcTemplate.queryForObject("SELECT id FROM users WHERE provider_id = 'g-new'", Long::class.java) shouldBe 999L
    jdbcTemplate.queryForObject("SELECT nickname FROM users WHERE provider_id = 'g-new'", String::class.java) shouldBe "User_new"
  }

  test("provider_id 가 다른 계정의 upsert 는 서로 다른 사용자를 만든다") {
    insertUser(1, "g-1")

    upsert("g-2")

    jdbcTemplate.queryForObject("SELECT count(*) FROM users", Int::class.java) shouldBe 2
    row("g-1")["email"] shouldBe "old1@example.com"
  }

  test("isDeactivatedAccount - soft-delete 된 계정은 true") {
    insertUser(1, "g-1", deleted = true)

    repository.isDeactivatedAccount(ProviderType.GOOGLE, "g-1") shouldBe true
  }

  test("isDeactivatedAccount - ACTIVE 가 아닌 계정은 true") {
    insertUser(1, "g-1", status = "INACTIVE")
    insertUser(2, "g-2", status = "DELETED")

    repository.isDeactivatedAccount(ProviderType.GOOGLE, "g-1") shouldBe true
    repository.isDeactivatedAccount(ProviderType.GOOGLE, "g-2") shouldBe true
  }

  test("isDeactivatedAccount - 활성 계정과 없는 계정은 false") {
    insertUser(1, "g-1")

    repository.isDeactivatedAccount(ProviderType.GOOGLE, "g-1") shouldBe false
    repository.isDeactivatedAccount(ProviderType.GOOGLE, "unknown") shouldBe false
  }

  test("다른 provider_id 가 이미 쓰는 email 로 upsert 하면 users_email_key 제약조건 위반으로 실패하고 기존 행은 그대로다") {
    insertUser(1, "g-1")
    val before = row("g-1")

    val thrown = shouldThrow<DataIntegrityViolationException> {
      tx.execute {
        repository.upsertByProvider(
          id = 999L,
          email = "old1@example.com",
          name = "Other",
          nickname = "User_other",
          provider = ProviderType.GOOGLE,
          providerId = "g-other",
          profileImage = null
        )
      }
    }

    generateSequence<Throwable>(thrown) { it.cause }
      .filterIsInstance<ConstraintViolationException>()
      .map { it.constraintName }
      .toList() shouldBe listOf("users_email_key")
    row("g-1") shouldBe before
  }
})
