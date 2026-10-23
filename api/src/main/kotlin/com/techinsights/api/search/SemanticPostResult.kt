package com.techinsights.api.search

data class SemanticPostResult(
    val post: PostSearchResultResponse,
    val similarityScore: Double,
    val rank: Int
)
