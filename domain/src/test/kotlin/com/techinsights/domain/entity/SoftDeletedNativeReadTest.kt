package com.techinsights.domain.entity

import com.techinsights.domain.dto.search.SearchRequest
import com.techinsights.domain.repository.post.PostEmbeddingJpaRepository
import com.techinsights.domain.repository.search.SearchRepository
import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.support.SqlFixtures
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Native SQL ignores `@SQLRestriction`, so every user-facing native read must filter soft-deleted posts and
 * companies itself, and the visible results must agree with the QueryDSL/JPQL reads and counts.
 *
 * Seed: post 1 is live; post 2 is soft-deleted; post 3 is live but its company (2) is soft-deleted.
 */
class SoftDeletedNativeReadTest : RepositoryTestSpec() {

  @Autowired lateinit var postEmbeddingJpaRepository: PostEmbeddingJpaRepository
  @Autowired lateinit var searchRepository: SearchRepository
  @Autowired lateinit var transactionManager: PlatformTransactionManager

  private val deletedAt = "TIMESTAMP '2024-01-01 00:00:00'"

  private fun seed() {
    val fixtures = SqlFixtures(jdbcTemplate)
    fixtures.insertCompany(1L)
    fixtures.insertCompany(2L)
    fixtures.insertPost(1L, companyId = 1L, title = "kotlin live")
    fixtures.insertPost(2L, companyId = 1L, title = "kotlin deleted post")
    fixtures.insertPost(3L, companyId = 2L, title = "kotlin deleted company")
    jdbcTemplate.update("UPDATE posts SET deleted_at = $deletedAt WHERE id = 2")
    jdbcTemplate.update("UPDATE company SET deleted_at = $deletedAt WHERE id = 2")
    (1L..3L).forEach { id ->
      jdbcTemplate.update(
        "INSERT INTO post_embedding (post_id, company_name, categories, content, embedding_vector, created_at, updated_at) " +
          "VALUES (?, 'c', 'c', 'x', CAST(? AS vector), now(), now())",
        id, fixtures.vectorLiteral(1f, id * 0.01f)
      )
    }
  }

  private val query get() = SqlFixtures(jdbcTemplate).vectorLiteral(1f)

  init {
    test("벡터 유사도 조회(findSimilarPostsAll)는 삭제된 게시글과 삭제된 회사의 게시글을 제외한다") {
      seed()

      postEmbeddingJpaRepository.findSimilarPostsAll(query, 10).map { it.postId } shouldBe listOf(1L)
    }

    test("벡터 유사도 조회(findSimilarPostsExcluding)도 삭제된 게시글과 회사의 게시글을 제외한다") {
      seed()

      postEmbeddingJpaRepository.findSimilarPostsExcluding(query, listOf(99L), 10).map { it.postId } shouldBe listOf(1L)
    }

    test("삭제된 행이 없는 게시글의 임베딩은 계속 조회된다") {
      seed()

      postEmbeddingJpaRepository.findSimilarPostsAll(query, 10).size shouldBe 1
    }

    test("즉시 검색과 전체 검색은 삭제된 게시글과 회사의 게시글을 제외하고 건수도 일치한다") {
      seed()

      TransactionTemplate(transactionManager).execute {
        val instant = searchRepository.instantSearch("kotlin", 10)
        instant.posts.size shouldBe 1

        val full = searchRepository.fullSearch(SearchRequest(query = "kotlin"))
        full.totalElements shouldBe 1L
        full.content.size shouldBe 1
      }
    }
  }
}
