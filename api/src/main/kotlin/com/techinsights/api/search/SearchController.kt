package com.techinsights.api.search

import com.techinsights.domain.dto.search.FullSearchResponse
import com.techinsights.domain.dto.search.InstantSearchResponse
import com.techinsights.domain.dto.search.SearchRequest
import com.techinsights.domain.enums.search.SearchSortType
import com.techinsights.domain.service.search.SearchService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
@RequestMapping("/api/v1/search")
class SearchController(
  private val searchService: SearchService,
  @param:Qualifier("ioDispatcher")private val ioDispatcher: CoroutineDispatcher
) {

  @GetMapping("/instant")
  suspend fun instantSearch(
    @RequestParam(defaultValue = "") query: String
  ): ResponseEntity<InstantSearchResponse> = withContext(ioDispatcher) {
    val response = searchService.instantSearch(query)
    ResponseEntity.ok(response)
  }

  @GetMapping
  suspend fun fullSearch(
    @RequestParam query: String,
    @RequestParam(defaultValue = "0") @Min(0) page: Int,
    @RequestParam(defaultValue = "10") @Min(1) @Max(100) size: Int,
    @RequestParam(defaultValue = "RELEVANCE") sortBy: SearchSortType,
    @RequestParam(required = false) companyId: Long?
  ): ResponseEntity<FullSearchResponse> = withContext(ioDispatcher) {
    val request = SearchRequest(
      query = query,
      page = page,
      size = size,
      sortBy = sortBy,
      companyId = companyId
    )

    val response = searchService.fullSearch(request)
    ResponseEntity.ok(response)
  }
}
