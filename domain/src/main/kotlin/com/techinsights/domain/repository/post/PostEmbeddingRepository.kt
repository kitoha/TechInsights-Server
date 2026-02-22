package com.techinsights.domain.repository.post

import com.techinsights.domain.dto.embedding.PostEmbeddingDto

interface PostEmbeddingRepository {
  fun findByPostIdIn(postIds: List<Long>): List<PostEmbeddingDto>
  fun findSimilarPostsAll(targetVector: String, limit: Long): List<PostEmbeddingDto>
  fun findSimilarPostsExcluding(targetVector: String, excludeIds: List<Long>, limit: Long): List<PostEmbeddingDto>
}