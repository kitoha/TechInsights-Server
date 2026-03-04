package com.techinsights.api.github

import com.techinsights.domain.dto.github.GithubSemanticSearchResult

data class GithubSemanticSearchResponse(
    val query: String,
    val results: List<GithubSemanticSearchResult>,
    val totalReturned: Int,
    val processingTimeMs: Long,
)
