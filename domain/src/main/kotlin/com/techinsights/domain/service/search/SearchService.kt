package com.techinsights.domain.service.search

import com.techinsights.domain.config.search.SearchProperties
import com.techinsights.domain.dto.search.SearchRequest
import com.techinsights.domain.dto.search.FullSearchResponse
import com.techinsights.domain.dto.search.InstantSearchResponse
import com.techinsights.domain.repository.search.SearchRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchService(
  private val searchRepository: SearchRepository,
  private val searchProperties: SearchProperties
) {

  @Transactional(readOnly = true)
  fun instantSearch(query: String): InstantSearchResponse {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) {
      return InstantSearchResponse(normalizedQuery, emptyList(), emptyList())
    }

    return searchRepository.instantSearch(
      normalizedQuery,
      searchProperties.instant.limit
    )
  }

  @Transactional(readOnly = true)
  fun fullSearch(request: SearchRequest): FullSearchResponse {
    val validatedRequest = request
      .copy(query = request.query.trim())
      .validate(searchProperties.full.maxSize)

    val page = searchRepository.fullSearch(validatedRequest)

    return FullSearchResponse(
      query = validatedRequest.query,
      posts = page.content,
      totalCount = page.totalElements,
      currentPage = page.number,
      totalPages = page.totalPages,
      hasNext = page.hasNext()
    )
  }
}