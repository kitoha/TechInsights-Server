package com.techinsights.domain.repository.post

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.support.SqlFixtures
import com.techinsights.domain.utils.Tsid
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

class PostRepositoryWriteTest(
  private val repository: PostRepository,
  private val jdbc: JdbcTemplate
) : RepositoryTestSpec({

  val fixtures = SqlFixtures(jdbc)
  val company = CompanyDto(Tsid.encode(1L), "company-1", "https://blog.example.com", "logo.png")

  fun newPost(id: Long, url: String) = PostDto(
    id = Tsid.encode(id),
    title = "title-$id",
    preview = null,
    url = url,
    content = "content",
    publishedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
    company = company,
    categories = emptySet()
  )

  fun countByUrl(url: String): Int =
    jdbc.queryForObject("SELECT count(*) FROM posts WHERE url = ?", Int::class.java, url)!!

  beforeEach { fixtures.insertCompany(1L) }

  test("insertAllIgnoringDuplicates ignores a post whose url already exists") {
    fixtures.insertPost(1L, url = "https://blog.example.com/same")

    val inserted = repository.insertAllIgnoringDuplicates(
      listOf(newPost(2L, "https://blog.example.com/same"), newPost(3L, "https://blog.example.com/new"))
    )

    inserted shouldBe 1
    countByUrl("https://blog.example.com/same") shouldBe 1
    countByUrl("https://blog.example.com/new") shouldBe 1
  }

  test("insertAllIgnoringDuplicates ignores duplicate urls inside one call") {
    val inserted = repository.insertAllIgnoringDuplicates(
      listOf(newPost(1L, "https://blog.example.com/dup"), newPost(2L, "https://blog.example.com/dup"))
    )

    inserted shouldBe 1
    countByUrl("https://blog.example.com/dup") shouldBe 1
  }

  test("insertAllIgnoringDuplicates accepts titles up to 500 characters") {
    val longTitle = "t".repeat(500)

    repository.insertAllIgnoringDuplicates(
      listOf(newPost(1L, "https://blog.example.com/long").copy(title = longTitle))
    ) shouldBe 1

    jdbc.queryForObject("SELECT title FROM posts WHERE id = 1", String::class.java) shouldBe longTitle
  }

  test("updateSummaryResults leaves view_count, like_count and summary_failure_count intact") {
    fixtures.insertPost(
      1L, viewCount = 42, likeCount = 9, summaryFailureCount = 3, isSummary = false, categories = listOf("AI")
    )
    val summarized = newPost(1L, "https://blog.example.com/1").copy(
      content = "summary",
      preview = "preview",
      isSummary = true,
      categories = setOf(Category.BackEnd)
    )

    repository.updateSummaryResults(listOf(summarized))

    val row = jdbc.queryForMap("SELECT content, preview, is_summary, view_count, like_count, summary_failure_count FROM posts WHERE id = 1")
    row["content"] shouldBe "summary"
    row["preview"] shouldBe "preview"
    row["is_summary"] shouldBe true
    row["view_count"] shouldBe 42L
    row["like_count"] shouldBe 9L
    row["summary_failure_count"] shouldBe 3
    jdbc.queryForList("SELECT category FROM post_categories WHERE post_id = 1", String::class.java) shouldBe listOf("BackEnd")
  }

  test("incrementViewCount adds one to the given post only") {
    fixtures.insertPost(1L, viewCount = 5)
    fixtures.insertPost(2L, viewCount = 5)

    repository.incrementViewCount(1L)

    jdbc.queryForObject("SELECT view_count FROM posts WHERE id = 1", Long::class.java) shouldBe 6L
    jdbc.queryForObject("SELECT view_count FROM posts WHERE id = 2", Long::class.java) shouldBe 5L
  }
})
