package com.techinsights.api.github

import com.techinsights.domain.service.github.GithubTrendingService
import com.techinsights.domain.utils.decode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class GithubCommunityController(
    private val githubTrendingService: GithubTrendingService,
    @param:Qualifier("ioDispatcher") private val ioDispatcher: CoroutineDispatcher,
) {

    @GetMapping("/api/v1/github/{id}/community")
    suspend fun getCommunity(
        @PathVariable id: String,
    ): ResponseEntity<GithubCommunityResponse> = withContext(ioDispatcher) {
        val repository = githubTrendingService.getRepositoryById(id.decode())
        ResponseEntity.ok(GithubCommunityResponse.fromDto(repository))
    }
}
