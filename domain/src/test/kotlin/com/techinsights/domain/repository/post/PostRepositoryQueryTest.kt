package com.techinsights.domain.repository.post

import com.techinsights.domain.enums.Category
import com.techinsights.domain.exception.PostNotFoundException
import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.support.SqlFixtures
import com.techinsights.domain.utils.Tsid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

class PostRepositoryQueryTest(
  private val repository: PostRepository,
  private val jdbc: JdbcTemplate,
  private val transactionTemplate: TransactionTemplate
) : RepositoryTestSpec({

  val fixtures = SqlFixtures(jdbc)
  val day = { d: Int -> LocalDateTime.of(2024, 1, d, 0, 0) }
  fun ids(vararg ids: Long) = ids.map { Tsid.encode(it) }
  // PostService reads inside @Transactional(readOnly = true); categories are lazy
  fun <T> readOnly(block: () -> T): T = transactionTemplate.execute { block() }!!

  fun column(name: String, id: Long) =
    jdbc.queryForObject("SELECT $name FROM posts WHERE id = ?", Long::class.java, id)

  beforeEach { fixtures.insertCompany(1L) }

  context("lookups") {
    test("findAllByUrlIn returns only posts with the given urls") {
      fixtures.insertPost(1L, url = "https://a.com/1")
      fixtures.insertPost(2L, url = "https://a.com/2")

      readOnly { repository.findAllByUrlIn(listOf("https://a.com/2", "https://a.com/none")) }
        .map { it.id } shouldContainExactly ids(2)
      repository.findAllByUrlIn(emptyList()).shouldBeEmpty()
    }

    test("findAllByIdIn decodes Tsid ids") {
      (1L..3L).forEach { fixtures.insertPost(it) }

      readOnly { repository.findAllByIdIn(ids(1, 3)) }.map { it.id } shouldContainExactlyInAnyOrder ids(1, 3)
    }

    test("getPostById returns the post with its company") {
      fixtures.insertPost(1L)

      val post = readOnly { repository.getPostById(Tsid.encode(1L)) }

      post.title shouldBe "title-1"
      post.company.id shouldBe Tsid.encode(1L)
    }

    test("getPostById throws when the post does not exist") {
      shouldThrow<PostNotFoundException> { repository.getPostById(Tsid.encode(404L)) }
    }

    test("existsById reflects whether the row exists") {
      fixtures.insertPost(1L)

      repository.existsById(1L) shouldBe true
      repository.existsById(2L) shouldBe false
    }

    test("getCompanyIdByPostId returns the encoded company id") {
      fixtures.insertPost(1L)

      repository.getCompanyIdByPostId(Tsid.encode(1L)) shouldBe Tsid.encode(1L)
      shouldThrow<PostNotFoundException> { repository.getCompanyIdByPostId(Tsid.encode(404L)) }
    }
  }

  context("batch readers") {
    test("findOldestNotSummarized returns unsummarized posts oldest first and skips exhausted retries") {
      fixtures.insertPost(1L, publishedAt = day(3), isSummary = false)
      fixtures.insertPost(2L, publishedAt = day(1), isSummary = false)
      fixtures.insertPost(3L, publishedAt = day(2), isSummary = false, summaryFailureCount = 5)
      fixtures.insertPost(4L, publishedAt = day(2), isSummary = true)

      readOnly { repository.findOldestNotSummarized(10) }.map { it.id } shouldContainExactly ids(2, 1)
    }

    test("findOldestNotSummarized pages with a (published_at, id) cursor and a limit") {
      fixtures.insertPost(1L, publishedAt = day(1), isSummary = false)
      fixtures.insertPost(2L, publishedAt = day(1), isSummary = false)
      fixtures.insertPost(3L, publishedAt = day(2), isSummary = false)

      readOnly { repository.findOldestNotSummarized(1) }.map { it.id } shouldContainExactly ids(1)
      readOnly { repository.findOldestNotSummarized(10, day(1), 1L) }.map { it.id } shouldContainExactly ids(2, 3)
      readOnly { repository.findOldestNotSummarized(10, day(1), 2L) }.map { it.id } shouldContainExactly ids(3)
    }

    test("findOldestSummarizedAndNotEmbedded returns summarized, unembedded posts oldest first") {
      fixtures.insertPost(1L, publishedAt = day(2), isSummary = true, isEmbedding = false)
      fixtures.insertPost(2L, publishedAt = day(1), isSummary = true, isEmbedding = false)
      fixtures.insertPost(3L, publishedAt = day(1), isSummary = true, isEmbedding = true)
      fixtures.insertPost(4L, publishedAt = day(1), isSummary = false)

      readOnly { repository.findOldestSummarizedAndNotEmbedded(10) }.map { it.id } shouldContainExactly ids(2, 1)
    }

    test("findOldestSummarized returns all summarized posts oldest first") {
      fixtures.insertPost(1L, publishedAt = day(2), isEmbedding = true)
      fixtures.insertPost(2L, publishedAt = day(1))
      fixtures.insertPost(3L, publishedAt = day(1), isSummary = false)

      readOnly { repository.findOldestSummarized(10) }.map { it.id } shouldContainExactly ids(2, 1)
    }

    test("updateEmbeddingStatusBulk marks the given posts as embedded") {
      (1L..3L).forEach { fixtures.insertPost(it) }

      repository.updateEmbeddingStatusBulk(ids(1, 2)) shouldBe 2L
      repository.updateEmbeddingStatusBulk(emptyList()) shouldBe 0L

      jdbc.queryForList("SELECT id FROM posts WHERE is_embedding ORDER BY id", Long::class.java) shouldBe listOf(1L, 2L)
    }

    test("incrementSummaryFailureCount adds one for the post") {
      fixtures.insertPost(1L, summaryFailureCount = 1)

      repository.incrementSummaryFailureCount(Tsid.encode(1L))

      column("summary_failure_count", 1L) shouldBe 2L
    }
  }

  context("statistics") {
    test("findTopViewedPosts returns summarized posts by view count desc up to the limit") {
      fixtures.insertPost(1L, viewCount = 10)
      fixtures.insertPost(2L, viewCount = 200)
      fixtures.insertPost(3L, viewCount = 50)
      fixtures.insertPost(4L, viewCount = 9999, isSummary = false)

      readOnly { repository.findTopViewedPosts(2) }.map { it.id } shouldContainExactly ids(2, 3)
    }

    test("getCategoryStatistics aggregates summarized posts per category without Category.All") {
      fixtures.insertPost(1L, viewCount = 10, publishedAt = day(1), categories = listOf("AI", "BackEnd"))
      fixtures.insertPost(2L, viewCount = 5, publishedAt = day(3), categories = listOf("AI"))
      fixtures.insertPost(3L, viewCount = 100, publishedAt = day(2), categories = listOf("AI"), isSummary = false)
      fixtures.insertPost(4L, viewCount = 1, publishedAt = day(1), categories = listOf("All"))

      val stats = repository.getCategoryStatistics().associateBy { it.category }

      stats.keys shouldContainExactlyInAnyOrder listOf(Category.AI, Category.BackEnd)
      stats.getValue(Category.AI).postCount shouldBe 2L
      stats.getValue(Category.AI).totalViewCount shouldBe 15L
      stats.getValue(Category.AI).latestPostDate shouldBe day(3)
      stats.getValue(Category.BackEnd).postCount shouldBe 1L
    }

    test("getAllPostSummary counts each summarized post once even with several categories") {
      fixtures.insertPost(1L, viewCount = 10, publishedAt = day(1), categories = listOf("AI", "BackEnd"))
      fixtures.insertPost(2L, viewCount = 5, publishedAt = day(3))
      fixtures.insertPost(3L, viewCount = 100, isSummary = false)

      val summary = repository.getAllPostSummary()

      summary.category shouldBe Category.All
      summary.postCount shouldBe 2L
      summary.totalViewCount shouldBe 15L
      summary.latestPostDate shouldBe day(3)
    }
  }

  context("like counts") {
    test("getLikeCount returns the stored count and throws for an unknown post") {
      fixtures.insertPost(1L, likeCount = 5)

      repository.getLikeCount(1L) shouldBe 5L
      shouldThrow<PostNotFoundException> { repository.getLikeCount(404L) }
    }

    test("incrementLikeCount adds one and throws for an unknown post") {
      fixtures.insertPost(1L, likeCount = 5)

      repository.incrementLikeCount(1L)

      column("like_count", 1L) shouldBe 6L
      shouldThrow<PostNotFoundException> { repository.incrementLikeCount(404L) }
    }

    test("decrementLikeCount subtracts one but never goes below zero") {
      fixtures.insertPost(1L, likeCount = 1)

      repository.decrementLikeCount(1L)

      column("like_count", 1L) shouldBe 0L
      shouldThrow<PostNotFoundException> { repository.decrementLikeCount(1L) }
      column("like_count", 1L) shouldBe 0L
    }
  }
})
