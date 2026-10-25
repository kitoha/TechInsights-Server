package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.RefreshToken
import com.techinsights.domain.support.RepositoryTestSpec
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

class RefreshTokenRepositoryTest(
  private val repository: RefreshTokenRepository,
  transactionManager: PlatformTransactionManager,
  jdbcTemplate: JdbcTemplate
) : RepositoryTestSpec({
  val tx = TransactionTemplate(transactionManager)
  fun <T> inTx(block: () -> T): T = tx.execute { block() }!!

  fun insertUser(id: Long) {
    jdbcTemplate.update(
      "INSERT INTO users (id, email, name, provider_id) VALUES (?, ?, 'n', ?)",
      id, "u$id@example.com", "p$id"
    )
  }

  fun session(
    id: Long,
    userId: Long = 1,
    familyId: Long = id * 10,
    hash: String = "h$id",
    deviceId: String? = null,
    expiry: Instant = Instant.now().plusSeconds(3600)
  ): RefreshToken = inTx {
    repository.save(RefreshToken(id = id, userId = userId, familyId = familyId, tokenHash = hash, deviceId = deviceId, expiryAt = expiry))
  }

  fun count(table: String): Int = jdbcTemplate.queryForObject("SELECT count(*) FROM $table", Int::class.java)!!

  test("현재 해시와 패밀리 ID 로 세션을 잠금 조회한다") {
    insertUser(1)
    session(1)

    inTx { repository.findByHashForUpdate("h1").get().id } shouldBe 1L
    inTx { repository.findByFamilyIdForUpdate(10).get().id } shouldBe 1L
    inTx { repository.findByHashForUpdate("unknown").isPresent } shouldBe false
  }

  test("폐기 해시를 기록하고 패밀리와 함께 조회한다 (같은 해시를 다시 기록해도 무시된다)") {
    insertUser(1)
    session(1)
    val at = Instant.now().minusSeconds(5)

    inTx { repository.retireHash("old", 10, at) }
    inTx { repository.retireHash("old", 10, Instant.now()) }

    val retired = inTx { repository.findRetiredByHash("old").get() }
    retired.familyId shouldBe 10L
    retired.retiredAt.epochSecond shouldBe at.epochSecond
    count("refresh_token_retired_hashes") shouldBe 1
  }

  test("패밀리의 가장 최근에 폐기된 해시를 조회한다") {
    insertUser(1)
    session(1)
    session(2, familyId = 20)
    inTx { repository.retireHash("older", 10, Instant.now().minusSeconds(100)) }
    inTx { repository.retireHash("newest", 10, Instant.now().minusSeconds(5)) }
    inTx { repository.retireHash("other-family", 20, Instant.now()) }

    repository.findLatestRetiredHash(10).get() shouldBe "newest"
    repository.findLatestRetiredHash(30).isPresent shouldBe false
  }

  test("세션을 삭제하면 그 패밀리의 폐기 해시도 함께 삭제된다") {
    insertUser(1)
    val s1 = session(1)
    session(2, familyId = 20)
    inTx { repository.retireHash("a", 10, Instant.now()) }
    inTx { repository.retireHash("b", 20, Instant.now()) }

    inTx { repository.delete(repository.findByHashForUpdate(s1.tokenHash).get()) }

    count("refresh_tokens") shouldBe 1
    inTx { repository.findRetiredByHash("a").isPresent } shouldBe false
    inTx { repository.findRetiredByHash("b").isPresent } shouldBe true
  }

  test("같은 사용자와 기기의 세션을 찾는다") {
    insertUser(1)
    insertUser(2)
    session(1, userId = 1, deviceId = "dev")
    session(2, userId = 2, deviceId = "dev")

    repository.findByUserAndDevice(1, "dev").get().id shouldBe 1L
    repository.findByUserAndDevice(1, "other").isPresent shouldBe false
  }

  test("사용자의 세션을 최근 사용 순(updated_at 내림차순)으로 조회한다") {
    insertUser(1)
    session(1); session(2); session(3)
    jdbcTemplate.update("UPDATE refresh_tokens SET updated_at = now() - interval '3 hours' WHERE id = 1")
    jdbcTemplate.update("UPDATE refresh_tokens SET updated_at = now() - interval '1 hours' WHERE id = 2")
    jdbcTemplate.update("UPDATE refresh_tokens SET updated_at = now() - interval '2 hours' WHERE id = 3")

    repository.findAllByUserIdOrderByLastUsedDesc(1).map { it.id } shouldBe listOf(2L, 3L, 1L)
  }

  test("사용자 ID 로 세션을 모두 삭제하며 다른 사용자의 세션은 남긴다") {
    insertUser(1)
    insertUser(2)
    session(1, userId = 1); session(2, userId = 1); session(3, userId = 2)

    repository.deleteAllByUserId(1)

    jdbcTemplate.queryForList("SELECT user_id FROM refresh_tokens", Long::class.java) shouldBe listOf(2L)
  }

  test("만료된 세션만 삭제한다") {
    insertUser(1)
    session(1, expiry = Instant.now().minusSeconds(60))
    session(2, expiry = Instant.now().plusSeconds(60))

    val deleted = repository.deleteExpired(Instant.now())

    deleted shouldBe 1
    jdbcTemplate.queryForList("SELECT id FROM refresh_tokens", Long::class.java) shouldBe listOf(2L)
  }

  test("기준 시각보다 오래된 폐기 해시만 삭제한다") {
    insertUser(1)
    session(1)
    inTx { repository.retireHash("old", 10, Instant.now().minusSeconds(86400 * 40)) }
    inTx { repository.retireHash("recent", 10, Instant.now()) }

    val deleted = repository.deleteRetiredBefore(Instant.now().minusSeconds(86400 * 30))

    deleted shouldBe 1
    inTx { repository.findRetiredByHash("recent").isPresent } shouldBe true
  }
})
