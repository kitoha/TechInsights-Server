package com.techinsights.api.github

import com.techinsights.domain.enums.GithubSortType
import com.techinsights.domain.service.github.GithubSemanticSearchService
import com.techinsights.domain.service.github.GithubTrendingService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class GithubTrendingController(
    private val githubTrendingService: GithubTrendingService,
    private val githubSemanticSearchService: GithubSemanticSearchService,
) {

    @GetMapping("/api/v1/github/trending")
    fun getTrendingRepos(
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(defaultValue = "STARS") sort: GithubSortType,
        @RequestParam(required = false) language: String?,
    ): ResponseEntity<CursorPageResponse<GithubRepositoryResponse>> {
        val result = githubTrendingService.getRepositoriesByCursor(cursor, size, sort, language)
        val content = result.content.map { GithubRepositoryResponse.fromDto(it) }
        return ResponseEntity.ok(
            CursorPageResponse(
                content = content,
                size = result.size,
                hasNext = result.hasNext,
                nextCursor = result.nextCursor,
            )
        )
    }

    @GetMapping("/api/v1/github/search")
    fun searchRepos(
        @RequestParam @NotBlank(message = "query must not be blank") @Size(max = MAX_QUERY_LENGTH) query: String,
        @RequestParam(defaultValue = "10") @Min(1) @Max(20) size: Int,
    ): ResponseEntity<GithubSemanticSearchResponse> {
        val trimmedQuery = query.trim()
        val startTime = System.currentTimeMillis()
        val results = githubSemanticSearchService.search(trimmedQuery, size)
        val processingTimeMs = System.currentTimeMillis() - startTime

        return ResponseEntity.ok(
            GithubSemanticSearchResponse(
                query = trimmedQuery,
                results = results.map { GithubSearchResultResponse.fromDto(it) },
                totalReturned = results.size,
                processingTimeMs = processingTimeMs,
            )
        )
    }

    companion object {
        private const val MAX_QUERY_LENGTH = 500
    }
}
