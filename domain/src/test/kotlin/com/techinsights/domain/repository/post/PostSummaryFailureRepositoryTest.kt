package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.PostSummaryFailure
import com.techinsights.domain.enums.SummaryErrorType
import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.support.SqlFixtures
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

class PostSummaryFailureRepositoryTest(
  private val repository: PostSummaryFailureRepository,
  private val jdbc: JdbcTemplate,
  private val transactionTemplate: TransactionTemplate
) : RepositoryTestSpec({

  val fixtures = SqlFixtures(jdbc)
  val at = { minute: Int -> LocalDateTime.of(2024, 1, 1, 0, minute) }

  fun failure(postId: Long, type: SummaryErrorType, failedAt: LocalDateTime, message: String? = null) =
    PostSummaryFailure(postId = postId, errorType = type, errorMessage = message, failedAt = failedAt)

  beforeEach {
    fixtures.insertCompany(1L)
    (1L..3L).forEach { fixtures.insertPost(it) }
  }

  test("save and saveAll persist failures") {
    repository.save(failure(1L, SummaryErrorType.API_ERROR, at(1)))
    repository.saveAll(
      listOf(failure(2L, SummaryErrorType.API_ERROR, at(2)), failure(3L, SummaryErrorType.CONTENT_ERROR, at(3)))
    )

    jdbc.queryForObject("SELECT count(*) FROM post_summary_failures", Int::class.java) shouldBe 3
  }

  test("findLatestByPostId returns the most recent failure of that post") {
    repository.saveAll(
      listOf(
        failure(1L, SummaryErrorType.API_ERROR, at(1), "old"),
        failure(1L, SummaryErrorType.CONTENT_ERROR, at(5), "new"),
        failure(2L, SummaryErrorType.API_ERROR, at(9), "other post")
      )
    )

    repository.findLatestByPostId(1L)?.errorMessage shouldBe "new"
    repository.findLatestByPostId(3L).shouldBeNull()
  }

  test("existsByPostIdAndErrorType matches both post and error type") {
    repository.save(failure(1L, SummaryErrorType.API_ERROR, at(1)))

    repository.existsByPostIdAndErrorType(1L, SummaryErrorType.API_ERROR) shouldBe true
    repository.existsByPostIdAndErrorType(1L, SummaryErrorType.CONTENT_ERROR) shouldBe false
    repository.existsByPostIdAndErrorType(2L, SummaryErrorType.API_ERROR) shouldBe false
  }

  test("findPostIdsWithErrorTypes returns each matching post once") {
    repository.saveAll(
      listOf(
        failure(1L, SummaryErrorType.API_ERROR, at(1)),
        failure(1L, SummaryErrorType.API_ERROR, at(2)),
        failure(2L, SummaryErrorType.CONTENT_ERROR, at(3)),
        failure(3L, SummaryErrorType.VALIDATION_ERROR, at(4))
      )
    )

    repository.findPostIdsWithErrorTypes(listOf(SummaryErrorType.API_ERROR, SummaryErrorType.CONTENT_ERROR))
      .shouldContainExactlyInAnyOrder(1L, 2L)
  }

  test("getErrorTypeStatisticsSince counts failures per error type from the given time") {
    repository.saveAll(
      listOf(
        failure(1L, SummaryErrorType.API_ERROR, at(1)),
        failure(1L, SummaryErrorType.API_ERROR, at(5)),
        failure(2L, SummaryErrorType.API_ERROR, at(6)),
        failure(3L, SummaryErrorType.CONTENT_ERROR, at(7))
      )
    )

    val stats = repository.getErrorTypeStatisticsSince(at(5)).associate { it.errorType to it.count }

    stats shouldBe mapOf(SummaryErrorType.API_ERROR to 2L, SummaryErrorType.CONTENT_ERROR to 1L)
  }

  test("findRecentFailures returns the newest failures first up to the limit") {
    repository.saveAll(
      listOf(
        failure(1L, SummaryErrorType.API_ERROR, at(1), "a"),
        failure(2L, SummaryErrorType.API_ERROR, at(3), "c"),
        failure(3L, SummaryErrorType.API_ERROR, at(2), "b")
      )
    )

    repository.findRecentFailures(2).map { it.errorMessage } shouldContainExactly listOf("c", "b")
  }

  test("deleteOldFailures removes only failures before the cutoff and reports the count") {
    repository.saveAll(
      listOf(
        failure(1L, SummaryErrorType.API_ERROR, at(1)),
        failure(2L, SummaryErrorType.API_ERROR, at(2)),
        failure(3L, SummaryErrorType.API_ERROR, at(3))
      )
    )

    // derived deletes need a surrounding transaction, which the callers provide
    val deleted = transactionTemplate.execute { repository.deleteOldFailures(at(3)) }

    deleted shouldBe 2
    jdbc.queryForList("SELECT post_id FROM post_summary_failures", Long::class.java) shouldBe listOf(3L)
  }
})
