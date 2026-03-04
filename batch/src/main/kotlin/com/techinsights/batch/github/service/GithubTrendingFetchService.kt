package com.techinsights.batch.github.service

import com.techinsights.batch.github.config.props.GithubApiProperties
import com.techinsights.batch.github.dto.GithubSearchResponse
import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class GithubTrendingFetchService(
    @Qualifier("githubWebClient") private val webClient: WebClient,
    private val rateLimiterRegistry: RateLimiterRegistry,
    private val props: GithubApiProperties,
) {
    private val rateLimiter by lazy { rateLimiterRegistry.rateLimiter("githubApi") }

    suspend fun fetchPage(query: String, page: Int): GithubSearchResponse =
        rateLimiter.executeSuspendFunction {
            webClient.get()
                .uri { uriBuilder ->
                    uriBuilder.path("/search/repositories")
                        .queryParam("q", query)
                        .queryParam("sort", "stars")
                        .queryParam("order", "desc")
                        .queryParam("per_page", props.perPage)
                        .queryParam("page", page)
                        .build()
                }
                .retrieve()
                .awaitBody()
        }
}
