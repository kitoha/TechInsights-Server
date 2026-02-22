package com.techinsights.domain.service.search

import com.techinsights.domain.dto.search.SemanticSearchResult

fun interface SemanticSearchService {
    fun search(query: String, size: Int, companyId: Long?): List<SemanticSearchResult>
}
