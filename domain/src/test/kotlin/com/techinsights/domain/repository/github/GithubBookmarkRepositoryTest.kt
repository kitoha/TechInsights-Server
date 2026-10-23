package com.techinsights.domain.repository.github

import com.techinsights.domain.entity.github.GithubBookmark
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

class GithubBookmarkRepositoryTest(
  private val repository: GithubBookmarkRepository,
  private val jdbc: JdbcTemplate,
  private val transactionTemplate: TransactionTemplate
) : RepositoryTestSpec({

  val fixtures = SqlFixtures(jdbc)
  val at = { minute: Int -> LocalDateTime.of(2024, 1, 1, 0, minute) }
  // GithubBookmarkService is @Transactional; derived deletes need a surrounding transaction
  fun <T> inTransaction(block: () -> T): T = transactionTemplate.execute { block() }!!

  beforeEach {
    (1L..3L).forEach { fixtures.insertGithubRepo(it) }
    fixtures.insertUser(1L)
    fixtures.insertUser(2L)
  }

  test("saveAndFlush stores a bookmark that can be found by repo and user") {
    repository.saveAndFlush(GithubBookmark(id = Tsid.generateLong(), repoId = 1L, userId = 1L))

    repository.findByRepoIdAndUserId(1L, 1L).shouldNotBeNull()
    repository.findByRepoIdAndUserId(1L, 2L).shouldBeNull()
  }

  test("deleteByRepoIdAndUserId removes only that bookmark and reports the count") {
    fixtures.insertGithubBookmark(1L, 1L, 1L, at(1))
    fixtures.insertGithubBookmark(2L, 2L, 1L, at(2))

    inTransaction { repository.deleteByRepoIdAndUserId(1L, 1L) } shouldBe 1L
    inTransaction { repository.deleteByRepoIdAndUserId(1L, 1L) } shouldBe 0L
    repository.countByUserId(1L) shouldBe 1L
  }

  test("findBookmarkedRepos returns the user's repos newest bookmark first with paging") {
    fixtures.insertGithubBookmark(1L, 1L, 1L, at(1))
    fixtures.insertGithubBookmark(2L, 3L, 1L, at(2))
    fixtures.insertGithubBookmark(3L, 2L, 1L, at(3))
    fixtures.insertGithubBookmark(4L, 2L, 2L, at(4))

    val first = repository.findBookmarkedRepos(1L, PageRequest.of(0, 2))
    val second = repository.findBookmarkedRepos(1L, PageRequest.of(1, 2))

    first.content.map { it.id } shouldContainExactly listOf(2L, 3L)
    first.totalElements shouldBe 3L
    second.content.map { it.id } shouldContainExactly listOf(1L)
  }

  test("countByUserId counts only that user's bookmarks") {
    fixtures.insertGithubBookmark(1L, 1L, 1L, at(1))
    fixtures.insertGithubBookmark(2L, 2L, 1L, at(2))
    fixtures.insertGithubBookmark(3L, 2L, 2L, at(3))

    repository.countByUserId(1L) shouldBe 2L
    repository.countByUserId(2L) shouldBe 1L
  }
})
