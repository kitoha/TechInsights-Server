package com.techinsights.domain.repository.post

import com.techinsights.domain.dto.embedding.PostEmbeddingDto

interface PostEmbeddingRepository {
  fun findByPostIdIn(postIds: List<Long>): List<PostEmbeddingDto>
  fun findSimilarPosts(targetVector: String, excludeIds: List<Long>, limit: Int): List<PostEmbeddingDto>
}