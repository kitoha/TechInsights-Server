package com.techinsights.domain.repository.company

import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.support.SqlFixtures
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate

class CompanyCountRepositoryTest(
  private val repository: CompanyRepository,
  private val jdbc: JdbcTemplate
) : RepositoryTestSpec({

  val fixtures = SqlFixtures(jdbc)

  fun postCount(companyId: Long): Long =
    jdbc.queryForObject("SELECT post_count FROM company WHERE id = ?", Long::class.java, companyId)!!

  fun totalViewCount(companyId: Long): Long =
    jdbc.queryForObject("SELECT total_view_count FROM company WHERE id = ?", Long::class.java, companyId)!!

  beforeEach {
    fixtures.insertCompany(1L)
    fixtures.insertCompany(2L)
  }

  test("incrementTotalViewCount adds one to the given company only") {
    repository.incrementTotalViewCount(1L)
    repository.incrementTotalViewCount(1L)

    totalViewCount(1L) shouldBe 2L
    totalViewCount(2L) shouldBe 0L
  }

  test("incrementPostCount adds the given count to the given company only") {
    repository.incrementPostCount(1L, 5)
    repository.incrementPostCount(1L, 2)

    postCount(1L) shouldBe 7L
    postCount(2L) shouldBe 0L
  }

  test("reconcilePostCounts recomputes post_count from summarized, non-deleted posts") {
    jdbc.update("UPDATE company SET post_count = 9 WHERE id = 1")
    jdbc.update("UPDATE company SET post_count = 4 WHERE id = 2")
    fixtures.insertPost(1L, companyId = 1L)
    fixtures.insertPost(2L, companyId = 1L)
    fixtures.insertPost(3L, companyId = 1L, isSummary = false)
    fixtures.insertPost(4L, companyId = 1L)
    jdbc.update("UPDATE posts SET deleted_at = now() WHERE id = 4")

    val corrected = repository.reconcilePostCounts()

    corrected shouldBe 2
    postCount(1L) shouldBe 2L
    postCount(2L) shouldBe 0L
  }

  test("reconcilePostCounts leaves already correct companies untouched") {
    fixtures.insertPost(1L, companyId = 1L)
    jdbc.update("UPDATE company SET post_count = 1 WHERE id = 1")

    repository.reconcilePostCounts() shouldBe 0
    postCount(1L) shouldBe 1L
  }
})
