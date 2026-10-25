package com.techinsights.domain.repository.post

import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.support.SqlFixtures
import com.techinsights.domain.utils.Tsid
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import org.springframework.jdbc.core.JdbcTemplate

class PostEmbeddingRepositoryTest(
  private val repository: PostEmbeddingRepository,
  private val jdbc: JdbcTemplate
) : RepositoryTestSpec({

  val fixtures = SqlFixtures(jdbc)
  val dimensions = 3072

  fun vectorLiteral(axis: Int, tilt: Float = 0f): String {
    val values = FloatArray(dimensions)
    values[axis] = 1f
    values[(axis + 1) % dimensions] = tilt
    return values.joinToString(prefix = "[", postfix = "]")
  }

  fun insertEmbedding(postId: Long, companyName: String, vector: String) {
    jdbc.update(
      """
      INSERT INTO post_embedding (post_id, company_name, categories, content, embedding_vector, created_at, updated_at)
      VALUES (?, ?, 'BackEnd', 'content', CAST(? AS vector), now(), now())
      """.trimIndent(),
      postId, companyName, vector
    )
  }

  test("findSimilarPostsByCompany returns that company's posts even when they are outside the global top-N") {
    val bigCompany = 1L
    val smallCompany = 2L
    fixtures.insertCompany(bigCompany, "big")
    fixtures.insertCompany(smallCompany, "small")

    // 10 posts of the big company are all closer to the query than the small company's posts
    (1L..10L).forEach { id ->
      fixtures.insertPost(id, companyId = bigCompany)
      insertEmbedding(id, "big", vectorLiteral(axis = 0, tilt = id * 0.01f))
    }
    fixtures.insertPost(101L, companyId = smallCompany)
    fixtures.insertPost(102L, companyId = smallCompany)
    insertEmbedding(101L, "small", vectorLiteral(axis = 1))
    insertEmbedding(102L, "small", vectorLiteral(axis = 2))

    val query = vectorLiteral(axis = 0)

    // premise: the global top-3 contains nothing of the small company
    repository.findSimilarPostsAll(query, 3).map { it.postId } shouldContainExactly
      listOf(1L, 2L, 3L).map { Tsid.encode(it) }

    val results = repository.findSimilarPostsByCompany(query, smallCompany, 3)

    results shouldHaveSize 2
    results.map { it.postId }.toSet() shouldContainExactly setOf(Tsid.encode(101L), Tsid.encode(102L))
  }

  test("findSimilarPostsByCompany orders by distance and respects the limit") {
    fixtures.insertCompany(1L, "c1")
    (1L..4L).forEach { id ->
      fixtures.insertPost(id, companyId = 1L)
      insertEmbedding(id, "c1", vectorLiteral(axis = 0, tilt = id * 0.1f))
    }

    val results = repository.findSimilarPostsByCompany(vectorLiteral(axis = 0), 1L, 2)

    results.map { it.postId } shouldContainExactly listOf(Tsid.encode(1L), Tsid.encode(2L))
  }
})
