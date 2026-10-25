package com.techinsights.domain.repository.user

import com.techinsights.domain.support.RepositoryTestSpec
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

class UserSessionLockTest(
  private val repository: RefreshTokenRepository,
  transactionManager: PlatformTransactionManager,
  jdbcTemplate: JdbcTemplate
) : RepositoryTestSpec({
  val tx = TransactionTemplate(transactionManager)

  // 'TISS' as an int: fixed namespace so the lock never shares the single-bigint key space with other advisory locks.
  val namespace = 1414681427

  fun advisoryLocks(): List<Map<String, Any?>> =
    jdbcTemplate.queryForList(
      "SELECT classid::bigint AS classid, objid::bigint AS objid, objsubid FROM pg_locks WHERE locktype = 'advisory'"
    )

  test("두 개의 int 형태(고정 namespace)로 잠그며 단일 bigint 키 공간을 쓰지 않는다") {
    tx.execute {
      repository.lockUserSessions(1)

      val locks = advisoryLocks()
      locks.size shouldBe 1
      (locks.single()["objsubid"] as Number).toInt() shouldBe 2
      (locks.single()["classid"] as Number).toInt() shouldBe namespace
    }

    advisoryLocks().size shouldBe 0
  }

  test("같은 사용자는 같은 잠금, 다른 사용자는 다른 잠금을 쓴다") {
    tx.execute {
      repository.lockUserSessions(1)
      repository.lockUserSessions(1)
      advisoryLocks().size shouldBe 1
      repository.lockUserSessions(2)
      advisoryLocks().size shouldBe 2
    }
  }
})
