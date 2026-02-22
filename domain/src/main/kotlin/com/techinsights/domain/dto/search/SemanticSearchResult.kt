package com.techinsights.domain.dto.search

import com.techinsights.domain.dto.post.PostDto

data class SemanticSearchResult(
    val post: PostDto,
    val similarityScore: Double,
    val rank: Int
)
