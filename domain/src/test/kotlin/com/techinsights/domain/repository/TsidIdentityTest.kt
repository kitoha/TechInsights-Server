package com.techinsights.domain.repository

import com.techinsights.domain.entity.post.PostSummaryFailure
import com.techinsights.domain.enums.SummaryErrorType
import com.techinsights.domain.repository.post.PostSummaryFailureRepository
import com.techinsights.domain.repository.user.AnonymousUserReadHistoryRepository
import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.support.SqlFixtures
import com.techinsights.domain.utils.Tsid
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate

class TsidIdentityTest(
  private val failureRepository: PostSummaryFailureRepository,
  private val readHistoryRepository: AnonymousUserReadHistoryRepository,
  private val jdbc: JdbcTemplate
) : RepositoryTestSpec({

  val fixtures = SqlFixtures(jdbc)

  beforeEach {
    fixtures.insertCompany(1L)
    fixtures.insertPost(1L)
  }

  test("read history rows get application-assigned Tsid ids") {
    readHistoryRepository.trackAnonymousPostRead("aid-1", Tsid.encode(1L))
    readHistoryRepository.trackAnonymousPostRead("aid-1", Tsid.encode(1L))

    val ids = jdbc.queryForList("SELECT id FROM anonymous_user_read_history ORDER BY id", Long::class.java)
    ids.size shouldBe 2
    ids.forEach { it shouldBeGreaterThan 1_000_000_000L }
  }

  test("summary failure rows get application-assigned Tsid ids") {
    failureRepository.save(PostSummaryFailure(postId = 1L, errorType = SummaryErrorType.UNKNOWN))

    val id = jdbc.queryForObject("SELECT id FROM post_summary_failures", Long::class.java)!!
    id shouldBeGreaterThan 1_000_000_000L
  }

  test("the id columns no longer have a serial default") {
    val defaults = jdbc.queryForList(
      """
      SELECT column_default FROM information_schema.columns
      WHERE table_name IN ('anonymous_user_read_history', 'post_summary_failures') AND column_name = 'id'
        AND column_default IS NOT NULL
      """.trimIndent(),
      String::class.java
    )
    defaults shouldBe emptyList()
  }
})
