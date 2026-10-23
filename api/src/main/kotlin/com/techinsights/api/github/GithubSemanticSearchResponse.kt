package com.techinsights.api.github

data class GithubSemanticSearchResponse(
    val query: String,
    val results: List<GithubSearchResultResponse>,
    val totalReturned: Int,
    val processingTimeMs: Long,
)
