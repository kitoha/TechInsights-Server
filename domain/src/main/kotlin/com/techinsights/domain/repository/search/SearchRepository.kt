package com.techinsights.domain.repository.search

import com.techinsights.domain.dto.search.InstantSearchResponse
import com.techinsights.domain.dto.search.PostSearchResultDto
import com.techinsights.domain.dto.search.SearchRequest
import org.springframework.data.domain.Page

interface SearchRepository {

  fun instantSearch(query: String, limit: Int): InstantSearchResponse
  fun fullSearch(request: SearchRequest): Page<PostSearchResultDto>
}