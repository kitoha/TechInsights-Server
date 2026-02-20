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

  override fun findSimilarPosts(
    targetVector: String,
    excludeIds: List<Long>,
    limit: Long
  ): List<PostEmbeddingDto> {
    val postEmbedding = postEmbeddingJpaRepository.findSimilarPosts(
      targetVector = targetVector,
      excludeIds = excludeIds,
      limit = limit
    )

    return postEmbedding.map { PostEmbeddingDto.fromEntity(it) }
  }
}
