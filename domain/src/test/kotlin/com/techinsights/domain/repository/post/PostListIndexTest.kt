package com.techinsights.domain.repository.post

import com.techinsights.domain.support.RepositoryTestSpec
import io.kotest.matchers.string.shouldContain
import org.springframework.jdbc.core.JdbcTemplate

class PostListIndexTest(
  private val jdbc: JdbcTemplate
) : RepositoryTestSpec({

  fun plan(sql: String): String =
    jdbc.queryForList("EXPLAIN $sql", String::class.java).joinToString("\n")

  beforeEach {
    jdbc.update(
      "INSERT INTO company (id, company_name, blog_url, logo_image_name, created_at, updated_at) VALUES (1, 'c', 'u', 'l', now(), now())"
    )
    jdbc.update(
      """
      INSERT INTO posts (id, title, url, content, published_at, company_id, view_count, is_summary, created_at, updated_at)
      SELECT g, 't', 'https://e.com/' || g, 'c', timestamp '2024-01-01' + g * interval '1 minute', 1,
             (random() * 100000)::bigint, g % 10 <> 0, now(), now()
      FROM generate_series(1, 30000) g
      """.trimIndent()
    )
    jdbc.execute("ANALYZE posts")
  }

  test("popular list is served by the view_count index") {
    val sql = """
      SELECT id FROM posts WHERE is_summary = true AND deleted_at IS NULL
      ORDER BY view_count DESC, id DESC LIMIT 10 OFFSET 0
    """.trimIndent()

    plan(sql) shouldContain "idx_posts_summary_popular"
  }

  test("recent list is served by the published_at cursor index") {
    val sql = """
      SELECT id FROM posts WHERE is_summary = true AND deleted_at IS NULL
      ORDER BY published_at DESC, id DESC LIMIT 10 OFFSET 0
    """.trimIndent()

    plan(sql) shouldContain "idx_posts_summary_cursor"
  }
})
