package com.techinsights.api.controller.search

import com.techinsights.domain.dto.search.FullSearchResponse
import com.techinsights.domain.dto.search.InstantSearchResponse
import com.techinsights.domain.dto.search.SearchRequest
import com.techinsights.domain.enums.search.SearchSortType
import com.techinsights.domain.service.search.SearchService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/search")
class SearchController(
  private val searchService: SearchService
) {

  @GetMapping("/instant")
  fun instantSearch(
    @RequestParam query: String
  ): ResponseEntity<InstantSearchResponse> {
    val response = searchService.instantSearch(query)
    return ResponseEntity.ok(response)
  }

  @GetMapping
  fun fullSearch(
    @RequestParam query: String,
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "10") size: Int,
    @RequestParam(defaultValue = "RELEVANCE") sortBy: SearchSortType,
    @RequestParam(required = false) companyId: Long?
  ): ResponseEntity<FullSearchResponse> {
    val request = SearchRequest(
      query = query,
      page = page,
      size = size,
      sortBy = sortBy,
      companyId = companyId
    )

    val response = searchService.fullSearch(request)
    return ResponseEntity.ok(response)
  }
}
