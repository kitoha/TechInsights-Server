package com.techinsights.domain.service.user

import com.techinsights.domain.dto.user.RefreshTokenRotationResult
import com.techinsights.domain.repository.user.RefreshTokenRepository
import com.techinsights.domain.support.RepositoryTestSpec
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class RefreshTokenServiceTest(
  repository: RefreshTokenRepository,
  transactionManager: PlatformTransactionManager,
  jdbcTemplate: JdbcTemplate
) : RepositoryTestSpec({
  val service = RefreshTokenService(repository)
  val tx = TransactionTemplate(transactionManager)
  val expiry = { Instant.now().plusSeconds(3600) }

  fun <T> inTx(block: () -> T): T = tx.execute { block() }!!

  fun insertUser(id: Long) {
    jdbcTemplate.update(
      "INSERT INTO users (id, email, name, provider_id) VALUES (?, ?, 'n', ?)",
      id, "u$id@example.com", "p$id"
    )
  }

  fun login(userId: Long, hash: String, deviceId: String? = null, maxSessions: Int = 10) =
    inTx { service.startSession(userId, deviceId, hash, expiry(), maxSessions) }

  // The domain only stores the sealed successor opaquely; the api seals it. Tests use a recognisable stand-in.
  fun sealed(new: String) = "sealed:$new"

  fun rotate(presented: String, new: String) = inTx { service.rotate(presented, new, expiry(), sealed(new)) }

  fun storedSuccessor(retiredHash: String): String? =
    jdbcTemplate.queryForList(
      "SELECT successor_ciphertext FROM refresh_token_retired_hashes WHERE token_hash = ?", String::class.java, retiredHash
    ).singleOrNull()

  fun sessionCount(userId: Long): Int =
    jdbcTemplate.queryForObject("SELECT count(*) FROM refresh_tokens WHERE user_id = ?", Int::class.java, userId)!!

  fun retiredCount(): Int =
    jdbcTemplate.queryForObject("SELECT count(*) FROM refresh_token_retired_hashes", Int::class.java)!!

  fun ageRetired(hash: String, seconds: Long) {
    jdbcTemplate.update(
      "UPDATE refresh_token_retired_hashes SET retired_at = now() - make_interval(secs => ?) WHERE token_hash = ?",
      seconds.toDouble(), hash
    )
  }

  fun currentHash(userId: Long): List<String> =
    jdbcTemplate.queryForList("SELECT token_hash FROM refresh_tokens WHERE user_id = ?", String::class.java, userId)

  context("rotation chain") {
    test("회전할 때마다 이전 해시가 같은 패밀리의 폐기 해시로 쌓이고 현재 해시만 유효하다") {
      insertUser(1)
      login(1, "h0")

      rotate("h0", "h1") shouldBe RefreshTokenRotationResult.Rotated
      rotate("h1", "h2") shouldBe RefreshTokenRotationResult.Rotated
      rotate("h2", "h3") shouldBe RefreshTokenRotationResult.Rotated

      currentHash(1) shouldBe listOf("h3")
      retiredCount() shouldBe 3
      jdbcTemplate.queryForObject(
        "SELECT count(DISTINCT family_id) FROM refresh_token_retired_hashes", Int::class.java
      ) shouldBe 1
    }

    test("처음 발급된 적 없는 해시는 NotFound이며 아무것도 폐기하지 않는다") {
      insertUser(1)
      login(1, "h0")

      rotate("never-issued", "x") shouldBe RefreshTokenRotationResult.NotFound

      currentHash(1) shouldBe listOf("h0")
    }
  }

  context("이전 토큰 재사용 감지") {
    test("두 번 이상 이전에 폐기된 토큰을 유예 시간 밖에서 재사용하면 해당 패밀리가 폐기된다") {
      insertUser(1)
      login(1, "h0")
      rotate("h0", "h1")
      rotate("h1", "h2")
      ageRetired("h0", 120)

      rotate("h0", "evil") shouldBe RefreshTokenRotationResult.ReuseDetected

      sessionCount(1) shouldBe 0
      retiredCount() shouldBe 0
    }

    test("재사용 감지는 같은 사용자의 다른 세션에 영향을 주지 않는다") {
      insertUser(1)
      login(1, "a0", deviceId = "dev-a")
      login(1, "b0", deviceId = "dev-b")
      rotate("a0", "a1")
      rotate("a1", "a2")
      ageRetired("a0", 120)

      rotate("a0", "evil") shouldBe RefreshTokenRotationResult.ReuseDetected

      currentHash(1) shouldBe listOf("b0")
      rotate("b0", "b1") shouldBe RefreshTokenRotationResult.Rotated
    }

    test("패밀리가 폐기된 뒤에는 그 패밀리의 현재 토큰도 사용할 수 없다") {
      insertUser(1)
      login(1, "h0")
      rotate("h0", "h1")
      ageRetired("h0", 120)
      rotate("h0", "evil")

      rotate("h1", "h2") shouldBe RefreshTokenRotationResult.NotFound
    }
  }

  context("유예 시간 (방금 폐기된 해시 = 같은 후속 토큰을 다시 전달, 새 회전 없음)") {
    test("회전하면 폐기된 해시 행에 후속 토큰의 봉인된 값이 저장된다") {
      insertUser(1)
      login(1, "h0")

      rotate("h0", "h1")

      storedSuccessor("h0") shouldBe sealed("h1")
    }

    test("응답 유실 복구: 회전 후 응답이 사라져도 유예 시간 안에 같은 폐기 토큰을 다시 제시하면 같은 후속 토큰을 돌려받는다") {
      insertUser(1)
      login(1, "h0")
      rotate("h0", "h1") shouldBe RefreshTokenRotationResult.Rotated

      rotate("h0", "unused-a") shouldBe RefreshTokenRotationResult.GraceRedelivery(sealed("h1"))
      rotate("h0", "unused-b") shouldBe RefreshTokenRotationResult.GraceRedelivery(sealed("h1"))

      currentHash(1) shouldBe listOf("h1")
      retiredCount() shouldBe 1
      // 이후 받은 R2(h1)로 정상 갱신되며 폐기되지 않는다
      rotate("h1", "h2") shouldBe RefreshTokenRotationResult.Rotated
      sessionCount(1) shouldBe 1
    }

    test("유예 시간이 지난 뒤 방금 폐기된 토큰을 제시하면 패밀리가 폐기된다") {
      insertUser(1)
      login(1, "h0")
      rotate("h0", "h1")
      ageRetired("h0", 120)

      rotate("h0", "evil") shouldBe RefreshTokenRotationResult.ReuseDetected

      sessionCount(1) shouldBe 0
      retiredCount() shouldBe 0
    }

    test("방금 폐기된 해시가 아닌 더 오래된 폐기 해시는 유예 시간 안이어도 패밀리를 폐기한다") {
      insertUser(1)
      login(1, "h0")
      rotate("h0", "h1")
      rotate("h1", "h2")

      rotate("h0", "evil") shouldBe RefreshTokenRotationResult.ReuseDetected

      sessionCount(1) shouldBe 0
    }

    test("재사용 폐기는 같은 사용자의 다른 세션에 영향을 주지 않는다") {
      insertUser(1)
      login(1, "a0", deviceId = "dev-a")
      login(1, "b0", deviceId = "dev-b")
      rotate("a0", "a1")
      rotate("a1", "a2")

      rotate("a0", "evil") shouldBe RefreshTokenRotationResult.ReuseDetected

      currentHash(1) shouldBe listOf("b0")
    }

    test("같은 토큰으로 동시에 여러 번 갱신하면 회전은 한 번이고 모든 응답이 같은 후속 토큰을 담는다") {
      insertUser(1)
      login(1, "h0")
      val threads = 6
      val ready = CountDownLatch(threads)
      val pool = Executors.newFixedThreadPool(threads)
      val results = (1..threads).map { i ->
        pool.submit<RefreshTokenRotationResult> {
          ready.countDown()
          ready.await()
          rotate("h0", "n$i")
        }
      }.map { it.get() }
      pool.shutdown()

      val winner = currentHash(1).single()
      results.count { it == RefreshTokenRotationResult.Rotated } shouldBe 1
      results.filterIsInstance<RefreshTokenRotationResult.GraceRedelivery>().map { it.sealedSuccessor }
        .toSet() shouldBe setOf(sealed(winner))
      results.count { it is RefreshTokenRotationResult.GraceRedelivery } shouldBe threads - 1
      sessionCount(1) shouldBe 1
      retiredCount() shouldBe 1
      rotate(winner, "next") shouldBe RefreshTokenRotationResult.Rotated
    }

    test("저장된 후속 토큰이 없는 (이전 버전에서 넘어온) 폐기 행은 재전달할 수 없으므로 패밀리를 폐기한다") {
      insertUser(1)
      login(1, "h0")
      rotate("h0", "h1")
      jdbcTemplate.update("UPDATE refresh_token_retired_hashes SET successor_ciphertext = NULL")

      rotate("h0", "x") shouldBe RefreshTokenRotationResult.ReuseDetected

      sessionCount(1) shouldBe 0
    }
  }

  context("봉인된 후속 토큰의 수명") {
    test("다음 회전 때 유예 시간이 지난 이전 폐기 행의 봉인 값은 지워진다") {
      insertUser(1)
      login(1, "h0")
      rotate("h0", "h1")
      ageRetired("h0", 120)

      rotate("h1", "h2")

      storedSuccessor("h0") shouldBe null
      storedSuccessor("h1") shouldBe sealed("h2")
    }

    test("정리 작업은 유예 시간이 지난 봉인 값만 지운다") {
      insertUser(1)
      login(1, "h0")
      rotate("h0", "h1")
      rotate("h1", "h2")
      ageRetired("h0", 120)

      val cleared = inTx { service.clearExpiredSuccessors() }

      cleared shouldBe 1
      storedSuccessor("h0") shouldBe null
      storedSuccessor("h1") shouldBe sealed("h2")
      retiredCount() shouldBe 2
    }
  }

  context("만료") {
    test("만료된 현재 토큰은 세션과 폐기 해시를 삭제하고 Expired를 반환한다") {
      insertUser(1)
      login(1, "h0")
      rotate("h0", "h1")
      jdbcTemplate.update("UPDATE refresh_tokens SET expiry_at = now() - interval '1 minute'")

      rotate("h1", "h2") shouldBe RefreshTokenRotationResult.Expired

      sessionCount(1) shouldBe 0
      retiredCount() shouldBe 0
    }
  }

  context("로그인(세션 시작)") {
    test("같은 기기로 다시 로그인하면 기존 세션과 폐기 해시를 지우고 새 패밀리를 시작한다") {
      insertUser(1)
      login(1, "h0", deviceId = "dev")
      rotate("h0", "h1")

      login(1, "fresh", deviceId = "dev")

      currentHash(1) shouldBe listOf("fresh")
      retiredCount() shouldBe 0
      rotate("h0", "x") shouldBe RefreshTokenRotationResult.NotFound
    }

    test("기기 ID가 없는 로그인은 항상 별도의 세션을 만든다") {
      insertUser(1)
      login(1, "a")
      login(1, "b")

      sessionCount(1) shouldBe 2
    }

    test("세션 상한을 넘으면 가장 오래 사용되지 않은 세션부터 삭제한다") {
      insertUser(1)
      login(1, "s1", maxSessions = 3)
      login(1, "s2", maxSessions = 3)
      login(1, "s3", maxSessions = 3)
      jdbcTemplate.update("UPDATE refresh_tokens SET updated_at = now() - interval '3 hours' WHERE token_hash = 's1'")
      jdbcTemplate.update("UPDATE refresh_tokens SET updated_at = now() - interval '2 hours' WHERE token_hash = 's2'")
      jdbcTemplate.update("UPDATE refresh_tokens SET updated_at = now() - interval '1 hours' WHERE token_hash = 's3'")
      // s1 을 회전시켜 가장 최근에 사용된 세션으로 만든다
      rotate("s1", "s1b")

      login(1, "s4", maxSessions = 3)

      currentHash(1).toSet() shouldBe setOf("s1b", "s3", "s4")
    }

    test("상한 초과로 삭제된 세션의 폐기 해시도 함께 삭제된다") {
      insertUser(1)
      login(1, "s1", maxSessions = 1)
      rotate("s1", "s1b")

      login(1, "s2", maxSessions = 1)

      currentHash(1) shouldBe listOf("s2")
      retiredCount() shouldBe 0
    }

    test("같은 사용자의 동시 로그인에서도 세션 상한을 넘지 않는다") {
      insertUser(1)
      val threads = 12
      val ready = CountDownLatch(threads)
      val pool = Executors.newFixedThreadPool(threads)
      val futures = (1..threads).map { i ->
        pool.submit {
          ready.countDown()
          ready.await()
          login(1, "c$i", maxSessions = 3)
        }
      }
      futures.forEach { it.get() }
      pool.shutdown()

      sessionCount(1) shouldBe 3
    }

    test("같은 기기로 동시에 로그인해도 세션은 하나만 남는다") {
      insertUser(1)
      val threads = 8
      val ready = CountDownLatch(threads)
      val pool = Executors.newFixedThreadPool(threads)
      val futures = (1..threads).map { i ->
        pool.submit {
          ready.countDown()
          ready.await()
          login(1, "d$i", deviceId = "same-device")
        }
      }
      futures.forEach { it.get() }
      pool.shutdown()

      sessionCount(1) shouldBe 1
    }

    test("상한은 사용자별로 적용된다") {
      insertUser(1)
      insertUser(2)
      login(1, "u1a", maxSessions = 1)
      login(2, "u2a", maxSessions = 1)

      login(1, "u1b", maxSessions = 1)

      currentHash(1) shouldBe listOf("u1b")
      currentHash(2) shouldBe listOf("u2a")
    }
  }

  context("로그아웃") {
    test("현재 해시로 로그아웃하면 세션이 삭제된다") {
      insertUser(1)
      login(1, "h0")

      inTx { service.deleteByHash("h0") }

      sessionCount(1) shouldBe 0
    }

    test("유예 시간 안의 폐기된 해시로도 세션을 종료할 수 있다") {
      insertUser(1)
      login(1, "h0")
      rotate("h0", "h1")

      inTx { service.deleteByHash("h0") }

      sessionCount(1) shouldBe 0
    }

    test("유예 시간이 지난 폐기 해시로 로그아웃하면 재사용으로 보고 해당 패밀리를 폐기한다") {
      insertUser(1)
      login(1, "a0", deviceId = "dev-a")
      login(1, "b0", deviceId = "dev-b")
      rotate("a0", "a1")
      ageRetired("a0", 120)

      inTx { service.deleteByHash("a0") }

      currentHash(1) shouldBe listOf("b0")
      retiredCount() shouldBe 0
    }

    test("알 수 없는 해시로 로그아웃하면 아무 세션도 지우지 않는다") {
      insertUser(1)
      login(1, "h0")

      inTx { service.deleteByHash("never-issued") }

      sessionCount(1) shouldBe 1
    }
  }

  context("정리") {
    test("만료된 세션과 그 폐기 해시를 삭제한다") {
      insertUser(1)
      login(1, "live")
      login(1, "dead")
      rotate("dead", "dead2")
      jdbcTemplate.update("UPDATE refresh_tokens SET expiry_at = now() - interval '1 day' WHERE token_hash = 'dead2'")

      val deleted = inTx { service.deleteExpired() }

      deleted shouldBe 1
      currentHash(1) shouldBe listOf("live")
      retiredCount() shouldBe 0
    }

    test("기준 시각보다 오래된 폐기 해시만 삭제한다") {
      insertUser(1)
      login(1, "h0")
      rotate("h0", "h1")
      rotate("h1", "h2")
      ageRetired("h0", 40L * 24 * 3600)

      val deleted = inTx { service.deleteRetiredBefore(Instant.now().minusSeconds(30L * 24 * 3600)) }

      deleted shouldBe 1
      retiredCount() shouldBe 1
    }
  }
})
