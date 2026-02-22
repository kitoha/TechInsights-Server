package com.techinsights.api.search

import com.techinsights.domain.config.search.SemanticSearchProperties
import com.techinsights.domain.dto.search.FullSearchResponse
import com.techinsights.domain.dto.search.InstantSearchResponse
import com.techinsights.domain.dto.search.SearchRequest
import com.techinsights.domain.enums.search.SearchSortType
import com.techinsights.domain.service.search.SearchService
import com.techinsights.domain.service.search.SemanticSearchService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
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
  private val semanticSearchService: SemanticSearchService,
  private val properties: SemanticSearchProperties,
  @param:Qualifier("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
) {

  @GetMapping("/instant")
  suspend fun instantSearch(
    @RequestParam @NotBlank(message = "query must not be blank") query: String
  ): ResponseEntity<InstantSearchResponse> = withContext(ioDispatcher) {
    val response = searchService.instantSearch(query)
    ResponseEntity.ok(response)
  }

  @GetMapping
  suspend fun fullSearch(
    @RequestParam @NotBlank(message = "query must not be blank") query: String,
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

  @GetMapping("/semantic")
  suspend fun semanticSearch(
    @RequestParam @NotBlank(message = "query must not be blank") @Size(max = MAX_QUERY_LENGTH) query: String,
    @RequestParam(required = false) @Min(1) size: Int?,
    @RequestParam(required = false) companyId: Long?
  ): ResponseEntity<SemanticSearchResponse> = withContext(ioDispatcher) {
    if (size != null && size > properties.maxSize) {
      throw IllegalArgumentException("size must not exceed maxSize(${properties.maxSize})")
    }

    val resolvedSize = size ?: properties.defaultSize
    val startTime = System.currentTimeMillis()

    val results = semanticSearchService.search(query.trim(), resolvedSize, companyId)
    val processingTimeMs = System.currentTimeMillis() - startTime

    ResponseEntity.ok(SemanticSearchResponse.of(query.trim(), results, processingTimeMs))
  }

  companion object {
    private const val MAX_QUERY_LENGTH = 500
  }
}
