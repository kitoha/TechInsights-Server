package com.techinsights.api.search

import com.techinsights.domain.config.search.SemanticSearchProperties
import com.techinsights.domain.dto.search.SearchRequest
import com.techinsights.domain.enums.search.SearchSortType
import com.techinsights.domain.service.search.SearchService
import com.techinsights.domain.service.search.SemanticSearchService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
@RequestMapping("/api/v1/search")
class SearchController(
  private val searchService: SearchService,
  private val semanticSearchService: SemanticSearchService,
  private val properties: SemanticSearchProperties
) {

  @GetMapping("/instant")
  fun instantSearch(
    @RequestParam @NotBlank(message = "query must not be blank") @Size(max = MAX_QUERY_LENGTH, message = "query must not exceed $MAX_QUERY_LENGTH characters") query: String
  ): ResponseEntity<InstantSearchResponse> {
    val response = searchService.instantSearch(query)
    return ResponseEntity.ok(InstantSearchResponse.fromDto(response))
  }

  @GetMapping
  fun fullSearch(
    @RequestParam @NotBlank(message = "query must not be blank") query: String,
    @RequestParam(defaultValue = "0") @Min(0) page: Int,
    @RequestParam(defaultValue = "10") @Min(1) @Max(100) size: Int,
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
    return ResponseEntity.ok(FullSearchResponse.fromDto(response))
  }

  @GetMapping("/semantic")
  fun semanticSearch(
    @RequestParam @NotBlank(message = "query must not be blank") @Size(max = MAX_QUERY_LENGTH) query: String,
    @RequestParam(required = false) @Min(1) size: Int?,
    @RequestParam(required = false) companyId: Long?
  ): ResponseEntity<SemanticSearchResponse> {
    if (size != null && size > properties.maxSize) {
      throw IllegalArgumentException("size must not exceed maxSize(${properties.maxSize})")
    }

    val resolvedSize = size ?: properties.defaultSize
    val startTime = System.currentTimeMillis()

    val results = semanticSearchService.search(query.trim(), resolvedSize, companyId)
    val processingTimeMs = System.currentTimeMillis() - startTime

    return ResponseEntity.ok(SemanticSearchResponse.of(query.trim(), results, processingTimeMs))
  }

  companion object {
    private const val MAX_QUERY_LENGTH = 500
  }
}
