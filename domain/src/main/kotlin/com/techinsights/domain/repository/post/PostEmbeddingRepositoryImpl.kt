package com.techinsights.domain.repository.post

import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.embedding.PostEmbeddingDto
import com.techinsights.domain.entity.post.QPostEmbedding
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class PostEmbeddingRepositoryImpl(
  private val postEmbeddingJpaRepository: PostEmbeddingJpaRepository,
  private val queryFactory: JPAQueryFactory,
  private val entityManager: EntityManager
) : PostEmbeddingRepository {

  override fun findByPostIdIn(postIds: List<Long>): List<PostEmbeddingDto> {
    val qPostEmbedding = QPostEmbedding.postEmbedding

    return queryFactory.selectFrom(qPostEmbedding)
      .where(qPostEmbedding.postId.`in`(postIds))
      .fetch()
      .map { PostEmbeddingDto.fromEntity(it) }
  }

  override fun findSimilarPostsAll(
    targetVector: String,
    limit: Long,
  ): List<PostEmbeddingDto> =
    postEmbeddingJpaRepository
      .findSimilarPostsAll(targetVector = targetVector, limit = limit)
      .map { PostEmbeddingDto.fromProjection(it) }

  @Transactional(readOnly = true)
  override fun findSimilarPostsByCompany(
    targetVector: String,
    companyId: Long,
    limit: Long,
  ): List<PostEmbeddingDto> {
    enableIterativeScan()
    return postEmbeddingJpaRepository
      .findSimilarPostsByCompany(targetVector = targetVector, companyId = companyId, limit = limit)
      .map { PostEmbeddingDto.fromProjection(it) }
  }

  /**
   * With a WHERE filter the HNSW index returns only ef_search candidates, which may all belong to other
   * companies. pgvector >= 0.8 can keep scanning (iterative scan); strict_order keeps results exactly ordered.
   * The setting only exists on pgvector >= 0.8 (and once the extension is loaded in the session), hence the guard.
   */
  private fun enableIterativeScan() {
    entityManager.createNativeQuery("SELECT count(*) FROM (SELECT '[1]'::vector) AS load_vector").singleResult
    entityManager.createNativeQuery(
      """
      SELECT set_config('hnsw.iterative_scan', 'strict_order', true)
      WHERE EXISTS (SELECT 1 FROM pg_settings WHERE name = 'hnsw.iterative_scan')
      """.trimIndent()
    ).resultList
  }

  override fun findSimilarPostsExcluding(
    targetVector: String,
    excludeIds: List<Long>,
    limit: Long,
  ): List<PostEmbeddingDto> =
    postEmbeddingJpaRepository
      .findSimilarPostsExcluding(targetVector = targetVector, excludeIds = excludeIds, limit = limit)
      .map { PostEmbeddingDto.fromProjection(it) }
}
