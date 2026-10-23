package com.techinsights.api.github

import com.techinsights.domain.enums.GithubSortType
import com.techinsights.domain.service.github.GithubTrendingService
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
    @GetMapping("/summary")
    fun getTrendingSummary(
        @RequestParam(required = false) language: String?,
    ): ResponseEntity<GithubSummaryResponse> {
        val summary = githubTrendingService.getSummary(language)
        return ResponseEntity.ok(GithubSummaryResponse.fromDto(summary))
    }
}
