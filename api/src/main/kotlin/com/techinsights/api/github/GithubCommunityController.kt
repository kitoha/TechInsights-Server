package com.techinsights.api.github

import com.techinsights.domain.service.github.GithubTrendingService
import com.techinsights.domain.utils.decode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class GithubCommunityController(
    private val githubTrendingService: GithubTrendingService,
) {

    @GetMapping("/api/v1/github/{id}/community")
    fun getCommunity(
        @PathVariable id: String,
    ): ResponseEntity<GithubCommunityResponse> {
        val repository = githubTrendingService.getRepositoryById(id.decode())
        return ResponseEntity.ok(GithubCommunityResponse.fromDto(repository))
    }
}
