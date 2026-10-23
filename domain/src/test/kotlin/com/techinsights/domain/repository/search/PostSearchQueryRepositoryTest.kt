package com.techinsights.domain.repository.search

import com.techinsights.domain.enums.search.SearchSortType
import com.techinsights.domain.repository.search.query.SearchQueryBuilder
import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.support.SqlFixtures
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

class PostSearchQueryRepositoryTest(
  private val repository: PostSearchQueryRepository,
  private val queryBuilder: SearchQueryBuilder,
  private val jdbc: JdbcTemplate
) : RepositoryTestSpec({

  val fixtures = SqlFixtures(jdbc)
  val day = { d: Int -> LocalDateTime.of(2020, 1, d, 0, 0) }

  beforeEach {
    fixtures.insertCompany(1L, "Kakao")
    fixtures.insertCompany(2L, "Naver")
  }

  fun instant(query: String, companyIds: Collection<Long> = emptyList(), limit: Int = 10) =
    repository.findForInstantSearch(query, companyIds, queryBuilder.buildRelevanceScore(query), limit)

  fun full(
    query: String,
    sort: SearchSortType,
    companyId: Long? = null,
    offset: Long = 0,
    limit: Long = 10
  ): List<Long> {
    val score = queryBuilder.buildRelevanceScore(query)
    return repository.findForFullSearch(
      condition = queryBuilder.buildSearchCondition(query, companyId),
      orderSpecifiers = queryBuilder.buildOrderSpecifier(sort, score),
      relevanceScore = score,
      offset = offset,
      limit = limit
    ).map { it.post.id }
  }

  context("instant search") {
    test("matches title or content case-insensitively on title and returns only summarized posts") {
      fixtures.insertPost(1L, title = "Kotlin Coroutines", companyId = 1L)
      fixtures.insertPost(2L, title = "Other", content = "all about kotlin", companyId = 1L)
      fixtures.insertPost(3L, title = "Spring", companyId = 1L)
      fixtures.insertPost(4L, title = "KOTLIN draft", companyId = 1L, isSummary = false)

      instant("kotlin").map { it.post.id } shouldContainExactlyInAnyOrder listOf(1L, 2L)
    }

    test("orders by relevance score and then view count, and applies the limit") {
      fixtures.insertPost(1L, title = "kotlin tips", viewCount = 1, publishedAt = day(1))
      fixtures.insertPost(2L, title = "about kotlin", viewCount = 1, publishedAt = day(1))
      fixtures.insertPost(3L, title = "about kotlin 2", viewCount = 5000, publishedAt = day(1))

      val result = instant("kotlin")

      result.first().post.id shouldBe 1L // title starts with the query: highest weight
      result.first().relevanceScore shouldBeGreaterThan result.last().relevanceScore
      instant("kotlin", limit = 2).size shouldBe 2
    }

    test("adds posts of matched companies by id") {
      fixtures.insertPost(1L, title = "unrelated", companyId = 2L)
      fixtures.insertPost(2L, title = "unrelated too", companyId = 1L)

      instant("zzz", companyIds = listOf(2L)).map { it.post.id } shouldContainExactly listOf(1L)
      instant("zzz").shouldBeEmpty()
    }
  }

  context("full search") {
    beforeEach {
      fixtures.insertPost(1L, title = "kotlin one", viewCount = 10, publishedAt = day(1), companyId = 1L)
      fixtures.insertPost(2L, title = "kotlin two", viewCount = 300, publishedAt = day(2), companyId = 2L)
      fixtures.insertPost(3L, title = "kotlin three", viewCount = 50, publishedAt = day(3), companyId = 1L)
      fixtures.insertPost(4L, title = "kotlin draft", viewCount = 999, publishedAt = day(4), isSummary = false)
    }

    test("LATEST orders by published date descending") {
      full("kotlin", SearchSortType.LATEST) shouldContainExactly listOf(3L, 2L, 1L)
    }

    test("POPULAR orders by view count descending") {
      full("kotlin", SearchSortType.POPULAR) shouldContainExactly listOf(2L, 3L, 1L)
    }

    test("company filter and paging apply") {
      full("kotlin", SearchSortType.LATEST, companyId = 1L) shouldContainExactly listOf(3L, 1L)
      full("kotlin", SearchSortType.LATEST, offset = 2, limit = 2) shouldContainExactly listOf(1L)
    }

    test("RELEVANCE puts the most viewed equally-matching post first") {
      full("kotlin", SearchSortType.RELEVANCE).first() shouldBe 2L
    }

    test("countByCondition counts summarized matches only") {
      repository.countByCondition(queryBuilder.buildSearchCondition("kotlin")) shouldBe 3L
      repository.countByCondition(queryBuilder.buildSearchCondition("kotlin", companyId = 1L)) shouldBe 2L
      repository.countByCondition(queryBuilder.buildSearchCondition("nothing")) shouldBe 0L
    }
  }

  test("countMatchedPostsByCompany counts summarized title or content matches of that company") {
    fixtures.insertPost(1L, title = "Kotlin", companyId = 1L)
    fixtures.insertPost(2L, title = "x", content = "kotlin inside", companyId = 1L)
    fixtures.insertPost(3L, title = "Kotlin", companyId = 2L)
    fixtures.insertPost(4L, title = "Kotlin", companyId = 1L, isSummary = false)

    repository.countMatchedPostsByCompany(1L, "kotlin") shouldBe 2L
    repository.countMatchedPostsByCompany(2L, "kotlin") shouldBe 1L
    repository.countMatchedPostsByCompany(1L, "rust") shouldBe 0L
  }
})
