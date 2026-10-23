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

  fun insertGithubRepo(
    id: Long,
    language: String? = "Kotlin",
    starCount: Long = 0L,
    weeklyStarDelta: Long = 0L,
    dailyStarDelta: Long = 0L,
    pushedAt: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0),
    topics: String? = null
  ) {
    jdbc.update(
      """
      INSERT INTO github_repositories (id, repo_name, full_name, html_url, star_count, primary_language, owner_name,
                                       topics, pushed_at, fetched_at, weekly_star_delta, daily_star_delta,
                                       created_at, updated_at)
      VALUES (?, ?, ?, ?, ?, ?, 'owner', ?, ?, now(), ?, ?, now(), now())
      """.trimIndent(),
      id, "repo-$id", "owner/repo-$id", "https://github.com/owner/repo-$id", starCount, language, topics,
      pushedAt, weeklyStarDelta, dailyStarDelta
    )
  }

  fun insertReadme(
    repoId: Long,
    summary: String? = null,
    summarizedAt: LocalDateTime? = null,
    errorType: String? = null,
    embeddedAt: LocalDateTime? = null,
    embeddingVector: String? = null
  ) {
    jdbc.update(
      """
      INSERT INTO github_repository_readme (repo_id, readme_summary, readme_summarized_at, readme_summary_error_type,
                                            readme_embedded_at, readme_embedding_vector)
      VALUES (?, ?, ?, ?, ?, CAST(? AS vector))
      """.trimIndent(),
      repoId, summary, summarizedAt, errorType, embeddedAt, embeddingVector
    )
  }

  fun insertCommunity(
    repoId: Long,
    status: String? = null,
    collectedAt: LocalDateTime? = null,
    rawMentionCount: Int = 0,
    mentionCount: Int = 0
  ) {
    jdbc.update(
      """
      INSERT INTO github_repository_community (repo_id, community_status, community_collected_at,
                                                community_raw_mention_count, community_mention_count)
      VALUES (?, ?, ?, ?, ?)
      """.trimIndent(),
      repoId, status, collectedAt, rawMentionCount, mentionCount
    )
  }

  fun insertGithubBookmark(id: Long, repoId: Long, userId: Long, createdAt: LocalDateTime) {
    jdbc.update(
      "INSERT INTO github_bookmarks (id, repo_id, user_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
      id, repoId, userId, createdAt, createdAt
    )
  }

  /** A 3072-dimension vector literal with [first] as its first component and zeros elsewhere. */
  fun vectorLiteral(first: Float, second: Float = 0f): String =
    (listOf(first, second) + List(3070) { 0f }).joinToString(",", "[", "]")
}
