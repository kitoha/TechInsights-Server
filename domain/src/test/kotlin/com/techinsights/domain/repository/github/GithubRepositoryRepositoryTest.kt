package com.techinsights.domain.repository.github

import com.techinsights.domain.dto.github.GithubRepositoryCursor
import com.techinsights.domain.enums.ErrorType
import com.techinsights.domain.enums.GithubSortType
import com.techinsights.domain.enums.CommunityStatus
import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.support.SqlFixtures
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

class GithubRepositoryRepositoryTest(
  private val repository: GithubRepositoryRepository,
  private val jdbc: JdbcTemplate
) : RepositoryTestSpec({

  val fixtures = SqlFixtures(jdbc)
  val day = { d: Int -> LocalDateTime.of(2024, 1, d, 0, 0) }

  context("list queries") {
    beforeEach {
      // stars:   1=100, 2=300, 3=300, 4=50     pushed: 1=d4, 2=d1, 3=d3, 4=d2
      // weekly:  1=5,   2=5,   3=9,   4=0      daily:  1=1,  2=7,  3=7,  4=0
      fixtures.insertGithubRepo(1L, "Kotlin", starCount = 100, weeklyStarDelta = 5, dailyStarDelta = 1, pushedAt = day(4))
      fixtures.insertGithubRepo(2L, "Kotlin", starCount = 300, weeklyStarDelta = 5, dailyStarDelta = 7, pushedAt = day(1))
      fixtures.insertGithubRepo(3L, "Java", starCount = 300, weeklyStarDelta = 9, dailyStarDelta = 7, pushedAt = day(3))
      fixtures.insertGithubRepo(4L, "Java", starCount = 50, weeklyStarDelta = 0, dailyStarDelta = 0, pushedAt = day(2))
      fixtures.insertReadme(1L, summary = "readme one")
    }

    fun page(sort: GithubSortType, language: String? = null, page: Int = 0, size: Int = 10) =
      repository.findRepositories(PageRequest.of(page, size), sort, language)

    test("STARS orders by stars then id descending and attaches the readme summary") {
      val result = page(GithubSortType.STARS)

      result.content.map { it.id } shouldContainExactly listOf(3L, 2L, 1L, 4L)
      result.content.last { it.id == 1L }.readmeSummary shouldBe "readme one"
      result.content.first { it.id == 2L }.readmeSummary.shouldBeNull()
      result.totalElements shouldBe 4L
    }

    test("LATEST orders by pushed_at descending") {
      page(GithubSortType.LATEST).content.map { it.id } shouldContainExactly listOf(1L, 3L, 4L, 2L)
    }

    test("TRENDING orders by weekly delta, then stars, then id") {
      page(GithubSortType.TRENDING).content.map { it.id } shouldContainExactly listOf(3L, 2L, 1L, 4L)
    }

    test("DAILY_TRENDING orders by daily delta, then stars, then id") {
      page(GithubSortType.DAILY_TRENDING).content.map { it.id } shouldContainExactly listOf(3L, 2L, 1L, 4L)
    }

    test("language filter limits both the rows and the total") {
      val result = page(GithubSortType.STARS, language = "Java")

      result.content.map { it.id } shouldContainExactly listOf(3L, 4L)
      result.totalElements shouldBe 2L
    }

    test("offset paging returns the requested slice") {
      val result = page(GithubSortType.STARS, page = 1, size = 3)

      result.content.map { it.id } shouldContainExactly listOf(4L)
      result.totalElements shouldBe 4L
    }

    test("an empty result is an empty page") {
      val result = page(GithubSortType.STARS, language = "Rust")

      result.content.shouldBeEmpty()
      result.totalElements shouldBe 0L
    }

    test("cursor paging by stars continues after the cursor without gaps or repeats") {
      val firstPage = repository.findRepositoriesByCursor(2, GithubSortType.STARS, null, null)
      val cursor = GithubRepositoryCursor.fromDto(firstPage.last(), GithubSortType.STARS)
      val secondPage = repository.findRepositoriesByCursor(2, GithubSortType.STARS, null, cursor)

      firstPage.map { it.id } shouldContainExactly listOf(3L, 2L)
      secondPage.map { it.id } shouldContainExactly listOf(1L, 4L)
    }

    test("cursor paging by trending uses delta, stars and id") {
      val firstPage = repository.findRepositoriesByCursor(2, GithubSortType.TRENDING, null, null)
      val cursor = GithubRepositoryCursor.fromDto(firstPage.last(), GithubSortType.TRENDING)

      repository.findRepositoriesByCursor(2, GithubSortType.TRENDING, null, cursor)
        .map { it.id } shouldContainExactly listOf(1L, 4L)
    }

    test("cursor paging by latest uses pushed_at and id") {
      val firstPage = repository.findRepositoriesByCursor(2, GithubSortType.LATEST, null, null)
      val cursor = GithubRepositoryCursor.fromDto(firstPage.last(), GithubSortType.LATEST)

      repository.findRepositoriesByCursor(2, GithubSortType.LATEST, null, cursor)
        .map { it.id } shouldContainExactly listOf(4L, 2L)
    }
  }

  context("findById") {
    test("returns the repository with readme and community details") {
      fixtures.insertGithubRepo(1L, topics = "kotlin, spring ,")
      fixtures.insertReadme(1L, summary = "summary")
      fixtures.insertCommunity(1L, status = "COMPLETED", collectedAt = day(2), mentionCount = 3)

      val dto = repository.findById(1L)!!

      dto.readmeSummary shouldBe "summary"
      dto.communityStatus shouldBe CommunityStatus.COMPLETED
      dto.communityCollectedAt shouldBe day(2)
      dto.communityMentionCount shouldBe 3
      dto.topics shouldContainExactly listOf("kotlin", "spring")
    }

    test("returns null for an unknown id") {
      repository.findById(404L).shouldBeNull()
    }
  }

  context("findUnsummarized") {
    beforeEach {
      fixtures.insertGithubRepo(1L, starCount = 300)
      fixtures.insertGithubRepo(2L, starCount = 200)
      fixtures.insertGithubRepo(3L, starCount = 100)
      fixtures.insertGithubRepo(4L, starCount = 50)
      fixtures.insertReadme(2L, summary = "done", summarizedAt = day(1))
      fixtures.insertReadme(3L, summarizedAt = day(1), errorType = "TIMEOUT")
      fixtures.insertReadme(4L, summarizedAt = day(5), errorType = "TIMEOUT")
    }

    test("returns only repositories that were never attempted, most stars first") {
      repository.findUnsummarized(10, null, null).map { it.id } shouldContainExactly listOf(1L)
    }

    test("also returns failed repositories whose retryable error is old enough") {
      repository.findUnsummarized(10, null, null, day(3), setOf(ErrorType.TIMEOUT))
        .map { it.id } shouldContainExactly listOf(1L, 3L)
    }

    test("ignores retryAfter when no retryable error types are given") {
      repository.findUnsummarized(10, null, null, day(3), emptySet()).map { it.id } shouldContainExactly listOf(1L)
    }

    test("the (stars, id) cursor skips already returned rows") {
      repository.findUnsummarized(10, 300, 1L, day(9), setOf(ErrorType.TIMEOUT))
        .map { it.id } shouldContainExactly listOf(3L, 4L)
    }

    test("pageSize limits the result") {
      repository.findUnsummarized(1, null, null, day(9), setOf(ErrorType.TIMEOUT))
        .map { it.id } shouldContainExactly listOf(1L)
    }
  }

  context("findUnembedded") {
    beforeEach {
      fixtures.insertGithubRepo(1L, starCount = 300)
      fixtures.insertGithubRepo(2L, starCount = 200)
      fixtures.insertGithubRepo(3L, starCount = 100)
      fixtures.insertGithubRepo(4L, starCount = 50)
      fixtures.insertReadme(1L, summary = "s1", summarizedAt = day(1))
      fixtures.insertReadme(2L, summary = "s2", summarizedAt = day(1), embeddedAt = day(2))
      fixtures.insertReadme(3L, summary = "s3", summarizedAt = day(1))
      fixtures.insertReadme(4L, summarizedAt = day(1))
    }

    test("returns summarized but unembedded repositories with their summary") {
      val result = repository.findUnembedded(10, null, null)

      result.map { it.id } shouldContainExactly listOf(1L, 3L)
      result.first().readmeSummary shouldBe "s1"
    }

    test("the cursor and page size apply") {
      repository.findUnembedded(10, 300, 1L).map { it.id } shouldContainExactly listOf(3L)
      repository.findUnembedded(1, null, null).map { it.id } shouldContainExactly listOf(1L)
    }
  }

  context("community queries") {
    beforeEach {
      (1L..5L).forEach { fixtures.insertGithubRepo(it) }
      fixtures.insertCommunity(1L, status = "COMPLETED", collectedAt = day(1), rawMentionCount = 8, mentionCount = 4)
      fixtures.insertCommunity(2L, status = "NO_MENTIONS", collectedAt = day(1))
      fixtures.insertCommunity(3L, status = "PENDING", collectedAt = day(8))
      fixtures.insertCommunity(4L, status = "COMPLETED", collectedAt = day(9))
      // repo 5 has no community row: never collected
    }

    test("findForCommunityCollect returns never collected first, then stale ones oldest first") {
      val result = repository.findForCommunityCollect(
        pageSize = 10, afterCollectedAt = null, afterId = null,
        noMentionsRefreshAfter = day(5), normalRefreshAfter = day(5)
      )

      result.map { it.id } shouldContainExactly listOf(5L, 1L, 2L)
      result.first { it.id == 1L }.communityRawMentionCount shouldBe 8
      result.first { it.id == 1L }.communityMentionCount shouldBe 4
    }

    test("findForCommunityCollect uses separate refresh thresholds for NO_MENTIONS and others") {
      val result = repository.findForCommunityCollect(
        pageSize = 10, afterCollectedAt = null, afterId = null,
        noMentionsRefreshAfter = day(1), normalRefreshAfter = day(10)
      )

      result.map { it.id } shouldContainExactly listOf(5L, 1L, 3L, 4L)
    }

    test("findForCommunityCollect honours the (collected_at, id) cursor and page size") {
      val cursorResult = repository.findForCommunityCollect(10, day(1), 1L, day(5), day(5))
      val limited = repository.findForCommunityCollect(1, null, null, day(5), day(5))

      cursorResult.map { it.id } shouldContainExactly listOf(2L)
      limited.map { it.id } shouldContainExactly listOf(5L)
    }

    test("findForCommunityAnalyze returns only PENDING repositories after the cursor") {
      repository.findForCommunityAnalyze(10, null).map { it.id } shouldContainExactly listOf(3L)
      repository.findForCommunityAnalyze(10, 3L).shouldBeEmpty()
    }
  }

  context("vector and summary") {
    test("findSimilarRepositories orders by cosine distance to the target vector") {
      fixtures.insertGithubRepo(1L)
      fixtures.insertGithubRepo(2L)
      fixtures.insertGithubRepo(3L)
      fixtures.insertReadme(1L, embeddingVector = fixtures.vectorLiteral(0f, 1f))
      fixtures.insertReadme(2L, embeddingVector = fixtures.vectorLiteral(1f, 0.1f))
      fixtures.insertReadme(3L) // no embedding

      val result = repository.findSimilarRepositories(fixtures.vectorLiteral(1f, 0f), 5)

      result.map { it.repoName } shouldContainExactly listOf("repo-2", "repo-1")
      (result[0].distance < result[1].distance) shouldBe true
    }

    test("countAndSumStars totals all repositories or one language") {
      fixtures.insertGithubRepo(1L, "Kotlin", starCount = 10)
      fixtures.insertGithubRepo(2L, "Kotlin", starCount = 20)
      fixtures.insertGithubRepo(3L, "Java", starCount = 5)

      repository.countAndSumStars(null).let { it.totalRepositories shouldBe 3L; it.totalStars shouldBe 35L }
      repository.countAndSumStars("Kotlin").let { it.totalRepositories shouldBe 2L; it.totalStars shouldBe 30L }
      repository.countAndSumStars("Rust").let { it.totalRepositories shouldBe 0L; it.totalStars shouldBe 0L }
    }
  }
})
