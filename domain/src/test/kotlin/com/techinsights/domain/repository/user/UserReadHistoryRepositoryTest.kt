package com.techinsights.domain.repository.user

import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.utils.Tsid
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.sql.Timestamp
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate

class UserReadHistoryRepositoryTest(
  private val repository: UserReadHistoryRepository,
  private val anonymousRepository: AnonymousUserReadHistoryRepository,
  jdbcTemplate: JdbcTemplate
) : RepositoryTestSpec({

  fun insertUser(id: Long) {
    jdbcTemplate.update(
      "INSERT INTO users (id, email, name, provider_id) VALUES (?, ?, 'n', ?)",
      id, "u$id@example.com", "p$id"
    )
  }

  fun backdate(userId: Long, postId: Long, minutesAgo: Int) {
    jdbcTemplate.update(
      "UPDATE user_read_history SET read_at = now() - make_interval(mins => ?) WHERE user_id = ? AND post_id = ?",
      minutesAgo, userId, postId
    )
  }

  test("저장한 읽기 이력을 최근 읽은 순으로 조회한다") {
    insertUser(1)
    repository.trackUserPostRead(1, Tsid.encode(10))
    repository.trackUserPostRead(1, Tsid.encode(20))
    repository.trackUserPostRead(1, Tsid.encode(30))
    backdate(1, 10, 30)
    backdate(1, 20, 10)
    backdate(1, 30, 20)

    val history = repository.getRecentReadHistory(1, PageRequest.of(0, 10))

    history.map { it.postId } shouldBe listOf("20", "30", "10")
    history.all { it.userId == 1L } shouldBe true
  }

  test("같은 글을 다시 읽으면 행이 늘지 않고 read_at만 갱신된다") {
    insertUser(1)
    repository.trackUserPostRead(1, Tsid.encode(10))
    backdate(1, 10, 60)
    val before = jdbcTemplate.queryForObject(
      "SELECT read_at FROM user_read_history WHERE user_id = 1 AND post_id = 10", Timestamp::class.java
    )!!

    repository.trackUserPostRead(1, Tsid.encode(10))

    jdbcTemplate.queryForObject("SELECT count(*) FROM user_read_history", Int::class.java) shouldBe 1
    val after = jdbcTemplate.queryForObject(
      "SELECT read_at FROM user_read_history WHERE user_id = 1 AND post_id = 10", Timestamp::class.java
    )!!
    after.after(before) shouldBe true
  }

  test("다시 읽은 글은 최근 읽은 순에서 맨 앞으로 오고 중복 없이 조회된다") {
    insertUser(1)
    repository.trackUserPostRead(1, Tsid.encode(10))
    repository.trackUserPostRead(1, Tsid.encode(20))
    backdate(1, 10, 30)
    backdate(1, 20, 10)

    repository.trackUserPostRead(1, Tsid.encode(10))

    repository.getRecentReadHistory(1, PageRequest.of(0, 10)).map { it.postId } shouldBe listOf("10", "20")
  }

  test("같은 글을 서로 다른 사용자가 읽으면 각자 한 행씩 가진다") {
    insertUser(1)
    insertUser(2)
    repository.trackUserPostRead(1, Tsid.encode(10))
    repository.trackUserPostRead(2, Tsid.encode(10))
    repository.trackUserPostRead(1, Tsid.encode(10))

    jdbcTemplate.queryForObject("SELECT count(*) FROM user_read_history", Int::class.java) shouldBe 2
  }

  test("Pageable 크기만큼만 조회한다") {
    insertUser(1)
    (1L..5L).forEach { repository.trackUserPostRead(1, Tsid.encode(it)) }

    repository.getRecentReadHistory(1, PageRequest.of(0, 3)).size shouldBe 3
  }

  test("다른 사용자와 익명 이력은 섞이지 않는다") {
    insertUser(1)
    insertUser(2)
    repository.trackUserPostRead(1, Tsid.encode(10))
    repository.trackUserPostRead(2, Tsid.encode(20))
    anonymousRepository.trackAnonymousPostRead("1", Tsid.encode(30))

    repository.getRecentReadHistory(1, PageRequest.of(0, 10)).map { it.postId } shouldBe listOf("10")
    repository.getRecentReadHistory(3, PageRequest.of(0, 10)).shouldBeEmpty()
  }

  test("사용자를 삭제하면 읽기 이력도 함께 삭제된다") {
    insertUser(1)
    repository.trackUserPostRead(1, Tsid.encode(10))

    jdbcTemplate.update("DELETE FROM users WHERE id = 1")

    jdbcTemplate.queryForObject("SELECT count(*) FROM user_read_history", Int::class.java) shouldBe 0
  }

  test("조회는 (user_id, read_at DESC) 인덱스를 사용할 수 있다") {
    insertUser(1)
    val indexDef = jdbcTemplate.queryForObject(
      "SELECT indexdef FROM pg_indexes WHERE indexname = 'idx_user_read_history_user_read_at'",
      String::class.java
    )!!

    indexDef.contains("(user_id, read_at DESC)") shouldBe true
  }
})
