package com.techinsights.domain.repository.post

import com.techinsights.domain.enums.Category
import com.techinsights.domain.enums.PostSortType
import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.support.SqlFixtures
import com.techinsights.domain.utils.Tsid
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

class PostRepositoryListQueryTest(
  private val repository: PostRepository,
  private val jdbc: JdbcTemplate,
  private val transactionTemplate: TransactionTemplate
) : RepositoryTestSpec({

  val fixtures = SqlFixtures(jdbc)
  val day = { d: Int -> LocalDateTime.of(2024, 1, d, 0, 0) }
  fun ids(vararg ids: Long) = ids.map { Tsid.encode(it) }
  // PostService reads these inside @Transactional(readOnly = true); categories are lazy
  fun <T> readOnly(block: () -> T): T = transactionTemplate.execute { block() }!!

  beforeEach {
    fixtures.insertCompany(1L)
    fixtures.insertCompany(2L)
    // newest -> oldest: 4, 3, 2, 1 ; most viewed: 2 (500), then 1 and 4 tied (100), then 3 (10)
    fixtures.insertPost(1L, publishedAt = day(1), viewCount = 100, categories = listOf("AI"))
    fixtures.insertPost(2L, publishedAt = day(2), viewCount = 500, categories = listOf("AI"))
    fixtures.insertPost(3L, publishedAt = day(3), viewCount = 10, categories = listOf("BackEnd"))
    fixtures.insertPost(4L, publishedAt = day(4), viewCount = 100, companyId = 2L, categories = listOf("AI"))
    fixtures.insertPost(5L, publishedAt = day(5), viewCount = 9999, isSummary = false)
  }

  val firstPage = PageRequest.of(0, 10)

  test("getAllPosts RECENT orders by published_at desc") {
    val result = readOnly { repository.getAllPosts(firstPage, null, PostSortType.RECENT) }

    result.content.map { it.id } shouldContainExactly ids(4, 3, 2, 1)
  }

  test("getAllPosts POPULAR orders by view_count desc with id desc as tie-break") {
    val result = readOnly { repository.getAllPosts(firstPage, null, PostSortType.POPULAR) }

    result.content.map { it.id } shouldContainExactly ids(2, 4, 1, 3)
  }

  test("getAllPosts POPULAR keeps the order across pages") {
    val secondPage = readOnly { repository.getAllPosts(PageRequest.of(1, 2), null, PostSortType.POPULAR) }

    secondPage.content.map { it.id } shouldContainExactly ids(1, 3)
    secondPage.totalElements shouldBe 4
  }

  test("getAllPosts POPULAR filters by company") {
    val result = readOnly { repository.getAllPosts(firstPage, Tsid.encode(1L), PostSortType.POPULAR) }

    result.content.map { it.id } shouldContainExactly ids(2, 1, 3)
  }

  test("getPostsByCategory POPULAR filters by company") {
    val result = readOnly {
      repository.getPostsByCategory(firstPage, Category.AI, Tsid.encode(1L), PostSortType.POPULAR)
    }

    result.content.map { it.id } shouldContainExactly ids(2, 1)
    result.totalElements shouldBe 2
  }

  test("getPostsByCategory RECENT orders by published_at desc") {
    val result = readOnly { repository.getPostsByCategory(firstPage, Category.AI, null, PostSortType.RECENT) }

    result.content.map { it.id } shouldContainExactly ids(4, 2, 1)
  }

  test("getPostsByCategory POPULAR orders by view_count desc with id desc as tie-break") {
    val result = readOnly { repository.getPostsByCategory(firstPage, Category.AI, null, PostSortType.POPULAR) }

    result.content.map { it.id } shouldContainExactly ids(2, 4, 1)
    result.totalElements shouldBe 3
  }
})
