package com.techinsights.domain.repository.post

import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.support.SqlFixtures
import io.kotest.matchers.shouldBe
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate

/** Re-runs the one-off V028 script against seeded data (Flyway already applied it to the empty schema). */
class ViewCountRecountMigrationTest(
  private val jdbc: JdbcTemplate
) : RepositoryTestSpec({

  val fixtures = SqlFixtures(jdbc)

  fun insertView(id: Long, postId: Long, identifier: String, day: String) {
    jdbc.update(
      "INSERT INTO post_view (id, post_id, user_or_ip, viewed_date, created_at, updated_at) VALUES (?, ?, ?, ?::date, now(), now())",
      id, postId, identifier, day
    )
  }

  fun viewCount(postId: Long) = jdbc.queryForObject("SELECT view_count FROM posts WHERE id = ?", Long::class.java, postId)
  fun companyTotal(companyId: Long) = jdbc.queryForObject("SELECT total_view_count FROM company WHERE id = ?", Long::class.java, companyId)

  test("recount sets view_count to the number of post_view rows and rebuilds company totals") {
    fixtures.insertCompany(1L)
    fixtures.insertCompany(2L)
    fixtures.insertPost(1L, companyId = 1L, viewCount = 10)
    fixtures.insertPost(2L, companyId = 1L, viewCount = 3)
    fixtures.insertPost(3L, companyId = 2L, viewCount = 5)
    insertView(1, 1, "a", "2024-01-01")
    insertView(2, 1, "b", "2024-01-01")
    insertView(3, 1, "a", "2024-01-02")
    insertView(4, 2, "a", "2024-01-01")
    jdbc.update("UPDATE company SET total_view_count = 999 WHERE id IN (1, 2)")

    val sql = ClassPathResource("db/migration/V028__Recount_View_Counts_After_Dedup.sql")
      .inputStream.bufferedReader().readText()
    jdbc.execute(sql)

    viewCount(1L) shouldBe 3L
    viewCount(2L) shouldBe 1L
    viewCount(3L) shouldBe 0L
    companyTotal(1L) shouldBe 4L
    companyTotal(2L) shouldBe 0L
  }
})
