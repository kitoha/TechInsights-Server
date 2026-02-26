package com.techinsights.api.github

import com.techinsights.api.post.PageResponse
import com.techinsights.domain.enums.GithubSortType
import com.techinsights.domain.service.github.GithubTrendingService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class GithubTrendingController(
    private val githubTrendingService: GithubTrendingService,
    @param:Qualifier("ioDispatcher") private val ioDispatcher: CoroutineDispatcher,
) {

    @GetMapping("/api/v1/github/trending")
    suspend fun getTrendingRepos(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(defaultValue = "STARS") sort: GithubSortType,
        @RequestParam(required = false) language: String?,
    ): ResponseEntity<PageResponse<GithubRepositoryResponse>> = withContext(ioDispatcher) {
        val result = githubTrendingService.getRepositories(page, size, sort, language)
        val content = result.content.map { GithubRepositoryResponse.fromDto(it) }
        ResponseEntity.ok(
            PageResponse(
                content = content,
                page = result.number,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            )
        )
    }
}
