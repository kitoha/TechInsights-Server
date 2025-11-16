package com.techinsights.domain.dto.search

import com.techinsights.domain.entity.post.Post

data class PostSearchProjection(
  val post: Post,
  val relevanceScore: Double
)
