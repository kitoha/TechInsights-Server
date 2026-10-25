package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.PostBookmark
import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.support.SqlFixtures
import com.techinsights.domain.utils.Tsid
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

class PostBookmarkRepositoryTest(
  private val repository: PostBookmarkRepository,
  private val jdbc: JdbcTemplate,
  private val transactionTemplate: TransactionTemplate
) : RepositoryTestSpec({

  val fixtures = SqlFixtures(jdbc)
  val at = { minute: Int -> LocalDateTime.of(2024, 1, 1, 0, minute) }
  // PostBookmarkService is @Transactional; derived deletes need a surrounding transaction
  fun <T> readOnly(block: () -> T): T = transactionTemplate.execute { block() }!!

  beforeEach {
    fixtures.insertCompany(1L)
    (1L..3L).forEach { fixtures.insertPost(it) }
    fixtures.insertUser(1L)
    fixtures.insertUser(2L)
  }

  test("saveAndFlush stores a bookmark that can be found by post and user") {
    repository.saveAndFlush(PostBookmark(id = Tsid.generateLong(), postId = 1L, userId = 1L))

    repository.findByPostIdAndUserId(1L, 1L).shouldNotBeNull()
    repository.findByPostIdAndUserId(1L, 2L).shouldBeNull()
  }

  test("deleteByPostIdAndUserId removes only that bookmark and reports the count") {
    fixtures.insertPostBookmark(1L, 1L, 1L, at(1))
    fixtures.insertPostBookmark(2L, 2L, 1L, at(2))

    readOnly { repository.deleteByPostIdAndUserId(1L, 1L) } shouldBe 1L
    readOnly { repository.deleteByPostIdAndUserId(1L, 1L) } shouldBe 0L
    repository.countByUserId(1L) shouldBe 1L
  }

  test("findBookmarkedPosts returns the user's bookmarked posts newest bookmark first") {
    fixtures.insertPostBookmark(1L, 1L, 1L, at(1))
    fixtures.insertPostBookmark(2L, 3L, 1L, at(2))
    fixtures.insertPostBookmark(3L, 2L, 1L, at(3))
    fixtures.insertPostBookmark(4L, 2L, 2L, at(4))

    val page = readOnly { repository.findBookmarkedPosts(1L, PageRequest.of(0, 2)) }

    page.content.map { it.id } shouldContainExactly listOf(Tsid.encode(2L), Tsid.encode(3L))
    page.totalElements shouldBe 3L
  }

  test("findBookmarkedPosts pages through the remaining bookmarks") {
    fixtures.insertPostBookmark(1L, 1L, 1L, at(1))
    fixtures.insertPostBookmark(2L, 3L, 1L, at(2))
    fixtures.insertPostBookmark(3L, 2L, 1L, at(3))

    val page = readOnly { repository.findBookmarkedPosts(1L, PageRequest.of(1, 2)) }

    page.content.map { it.id } shouldContainExactly listOf(Tsid.encode(1L))
  }

  test("countByUserId counts only that user's bookmarks") {
    fixtures.insertPostBookmark(1L, 1L, 1L, at(1))
    fixtures.insertPostBookmark(2L, 2L, 1L, at(2))
    fixtures.insertPostBookmark(3L, 2L, 2L, at(3))

    repository.countByUserId(1L) shouldBe 2L
    repository.countByUserId(2L) shouldBe 1L
  }
})
