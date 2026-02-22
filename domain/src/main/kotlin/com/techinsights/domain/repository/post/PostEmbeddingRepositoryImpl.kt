package com.techinsights.domain.repository.post

import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.embedding.PostEmbeddingDto
import com.techinsights.domain.entity.post.QPostEmbedding
import org.springframework.stereotype.Repository

@Repository
class PostEmbeddingRepositoryImpl(
  private val postEmbeddingJpaRepository: PostEmbeddingJpaRepository,
  private val queryFactory: JPAQueryFactory
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

  override fun findSimilarPostsExcluding(
    targetVector: String,
    excludeIds: List<Long>,
    limit: Long,
  ): List<PostEmbeddingDto> =
    postEmbeddingJpaRepository
      .findSimilarPostsExcluding(targetVector = targetVector, excludeIds = excludeIds, limit = limit)
      .map { PostEmbeddingDto.fromProjection(it) }
}
