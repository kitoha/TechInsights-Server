package com.techinsights.api.github

import com.techinsights.domain.dto.github.GithubSummaryDto
import com.techinsights.domain.enums.GithubSortType
import com.techinsights.domain.service.github.GithubTrendingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/github/trending")
class GithubTrendingSummaryController(
    private val githubTrendingService: GithubTrendingService
) {
    private val ioDispatcher = Dispatchers.IO

    @GetMapping("/summary")
    suspend fun getTrendingSummary(
        @RequestParam(defaultValue = "STARS") sort: GithubSortType,
        @RequestParam(required = false) language: String?,
    ): ResponseEntity<GithubSummaryDto> = withContext(ioDispatcher) {
        val summary = githubTrendingService.getSummary(sort, language)
        ResponseEntity.ok(summary)
    }
}
