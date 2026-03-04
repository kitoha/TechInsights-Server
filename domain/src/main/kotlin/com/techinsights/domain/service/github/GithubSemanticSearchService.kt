package com.techinsights.domain.service.github

import com.techinsights.domain.dto.github.GithubSemanticSearchResult

interface GithubSemanticSearchService {
    fun search(query: String, size: Int): List<GithubSemanticSearchResult>
}
