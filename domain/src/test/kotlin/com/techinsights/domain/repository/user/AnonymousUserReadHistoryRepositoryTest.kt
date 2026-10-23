package com.techinsights.domain.repository.user

import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.support.SqlFixtures
import com.techinsights.domain.utils.Tsid
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

class AnonymousUserReadHistoryRepositoryTest(
  private val repository: AnonymousUserReadHistoryRepository,
  private val jdbc: JdbcTemplate
) : RepositoryTestSpec({

  val fixtures = SqlFixtures(jdbc)
  val at = { minute: Int -> LocalDateTime.of(2024, 1, 1, 0, minute) }

  test("trackAnonymousPostRead stores the decoded post id for the anonymous identifier") {
    repository.trackAnonymousPostRead("aid-1", Tsid.encode(42L))

    val row = jdbc.queryForMap("SELECT anonymous_id, post_id FROM anonymous_user_read_history")
    row["anonymous_id"] shouldBe "aid-1"
    row["post_id"] shouldBe 42L
  }

  test("tracking the same post twice keeps both reads") {
    repository.trackAnonymousPostRead("aid-1", Tsid.encode(1L))
    repository.trackAnonymousPostRead("aid-1", Tsid.encode(1L))

    jdbc.queryForObject("SELECT count(*) FROM anonymous_user_read_history", Int::class.java) shouldBe 2
  }

  // RecommendationService parses these ids with toLongOrNull, so the DTO carries the raw numeric id
  test("getRecentReadHistory returns the identifier's reads newest first") {
    fixtures.insertAnonymousRead(1L, "aid-1", 10L, at(1))
    fixtures.insertAnonymousRead(2L, "aid-1", 11L, at(3))
    fixtures.insertAnonymousRead(3L, "aid-1", 12L, at(2))
    fixtures.insertAnonymousRead(4L, "aid-2", 99L, at(9))

    val result = repository.getRecentReadHistory("aid-1", PageRequest.of(0, 10))

    result.map { it.postId } shouldContainExactly listOf(11L, 12L, 10L).map { it.toString() }
    result.map { it.anonymousId }.toSet() shouldBe setOf("aid-1")
  }

  test("getRecentReadHistory honours the page size and offset") {
    (1..5).forEach { fixtures.insertAnonymousRead(it.toLong(), "aid-1", 100L + it, at(it)) }

    repository.getRecentReadHistory("aid-1", PageRequest.of(0, 2)).map { it.postId } shouldContainExactly
      listOf(105L, 104L).map { it.toString() }
    repository.getRecentReadHistory("aid-1", PageRequest.of(1, 2)).map { it.postId } shouldContainExactly
      listOf(103L, 102L).map { it.toString() }
  }

  test("getRecentReadHistory is empty for an unknown identifier") {
    fixtures.insertAnonymousRead(1L, "aid-1", 10L, at(1))

    repository.getRecentReadHistory("nobody", PageRequest.of(0, 10)).shouldBeEmpty()
  }
})
