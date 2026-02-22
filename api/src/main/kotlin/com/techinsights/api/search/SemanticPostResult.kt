package com.techinsights.api.search

import com.techinsights.domain.dto.search.PostSearchResultDto

data class SemanticPostResult(
    val post: PostSearchResultDto,
    val similarityScore: Double,
    val rank: Int
)
