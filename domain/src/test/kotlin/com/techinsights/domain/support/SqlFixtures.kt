package com.techinsights.domain.support

import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

/** Inserts rows with plain SQL so tests seed exactly the state they need, independent of entity mapping. */
class SqlFixtures(private val jdbc: JdbcTemplate) {

  fun insertCompany(id: Long = 1L, name: String = "company-$id") {
    jdbc.update(
      """
      INSERT INTO company (id, company_name, blog_url, logo_image_name, created_at, updated_at)
      VALUES (?, ?, 'https://blog.example.com', 'logo.png', now(), now())
      """.trimIndent(),
      id, name
    )
  }

  fun insertPost(
    id: Long,
    companyId: Long = 1L,
    url: String = "https://blog.example.com/$id",
    publishedAt: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0),
    viewCount: Long = 0L,
    likeCount: Long = 0L,
    summaryFailureCount: Int = 0,
    isSummary: Boolean = true,
    isEmbedding: Boolean = false,
    categories: List<String> = emptyList()
  ) {
    jdbc.update(
      """
      INSERT INTO posts (id, title, url, content, published_at, company_id, view_count, like_count,
                         summary_failure_count, is_summary, is_embedding, created_at, updated_at)
      VALUES (?, ?, ?, 'content', ?, ?, ?, ?, ?, ?, ?, now(), now())
      """.trimIndent(),
      id, "title-$id", url, publishedAt, companyId, viewCount, likeCount, summaryFailureCount, isSummary, isEmbedding
    )
    categories.forEachIndexed { index, category ->
      jdbc.update(
        "INSERT INTO post_categories (id, post_id, category) VALUES (?, ?, ?)",
        id * 100 + index, id, category
      )
    }
  }

  fun insertUser(id: Long) {
    jdbc.update(
      "INSERT INTO users (id, email, name, provider_id) VALUES (?, ?, ?, ?)",
      id, "user$id@example.com", "user-$id", "provider-$id"
    )
  }

  fun insertPostBookmark(id: Long, postId: Long, userId: Long, createdAt: LocalDateTime) {
    jdbc.update(
      "INSERT INTO post_bookmarks (id, post_id, user_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
      id, postId, userId, createdAt, createdAt
    )
  }
}
