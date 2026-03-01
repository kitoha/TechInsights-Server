package com.techinsights.batch.github.service

import com.techinsights.batch.github.config.props.GithubApiProperties
import com.techinsights.batch.github.dto.GithubSearchResponse
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration
import java.util.function.Function

class GithubTrendingFetchServiceTest : FunSpec({

    val webClient = mockk<WebClient>()
    val rateLimiterRegistry = mockk<RateLimiterRegistry>()
    val props = GithubApiProperties(perPage = 50)

    val uriSpec = mockk<WebClient.RequestHeadersUriSpec<*>>()
    val headersSpec = mockk<WebClient.RequestHeadersSpec<*>>()
    val responseSpec = mockk<WebClient.ResponseSpec>()

    val rateLimiter = RateLimiter.of(
        "githubApi-test",
        RateLimiterConfig.custom()
            .limitForPeriod(Int.MAX_VALUE)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ofSeconds(5))
            .build()
    )

    beforeTest {
        clearAllMocks()
        every { rateLimiterRegistry.rateLimiter("githubApi") } returns rateLimiter
        every { webClient.get() } returns uriSpec
        every { uriSpec.uri(any<Function<UriBuilder, URI>>()) } returns headersSpec
        every { headersSpec.retrieve() } returns responseSpec
    }

    test("fetchPage()는 GitHub Search API 응답을 GithubSearchResponse로 반환한다") {
        val mockResponse = GithubSearchResponse(
            totalCount = 2,
            items = listOf(
                createItem("owner/repo1", stargazersCount = 5000L),
                createItem("owner/repo2", stargazersCount = 3000L),
            )
        )
        every { responseSpec.bodyToMono(any<ParameterizedTypeReference<GithubSearchResponse>>()) } returns Mono.just(mockResponse)

        val service = GithubTrendingFetchService(webClient, rateLimiterRegistry, props)
        val result = service.fetchPage("stars:>100", 1)

        result.totalCount shouldBe 2
        result.items shouldHaveSize 2
        result.items[0].fullName shouldBe "owner/repo1"
        result.items[1].fullName shouldBe "owner/repo2"
    }

    test("fetchPage()는 빈 결과도 정상적으로 반환한다") {
        val mockResponse = GithubSearchResponse(totalCount = 0, items = emptyList())
        every { responseSpec.bodyToMono(any<ParameterizedTypeReference<GithubSearchResponse>>()) } returns Mono.just(mockResponse)

        val service = GithubTrendingFetchService(webClient, rateLimiterRegistry, props)
        val result = service.fetchPage("stars:>100", 1)

        result.totalCount shouldBe 0
        result.items.shouldHaveSize(0)
    }

    test("fetchPage()는 WebClientResponseException을 호출자에게 그대로 전파한다") {
        every { responseSpec.bodyToMono(any<ParameterizedTypeReference<GithubSearchResponse>>()) } returns
            Mono.error(
                WebClientResponseException.create(
                    403, "Forbidden", HttpHeaders.EMPTY, ByteArray(0), null
                )
            )

        val service = GithubTrendingFetchService(webClient, rateLimiterRegistry, props)

        shouldThrow<WebClientResponseException> {
            service.fetchPage("stars:>100", 1)
        }
    }

    test("fetchPage()는 503 Service Unavailable 예외도 전파한다") {
        every { responseSpec.bodyToMono(any<ParameterizedTypeReference<GithubSearchResponse>>()) } returns
            Mono.error(
                WebClientResponseException.create(
                    503, "Service Unavailable", HttpHeaders.EMPTY, ByteArray(0), null
                )
            )

        val service = GithubTrendingFetchService(webClient, rateLimiterRegistry, props)

        shouldThrow<WebClientResponseException> {
            service.fetchPage("stars:>100", 1)
        }
    }

    test("fetchPage()는 rateLimiterRegistry에서 'githubApi' RateLimiter를 조회한다") {
        val mockResponse = GithubSearchResponse(totalCount = 0, items = emptyList())
        every { responseSpec.bodyToMono(any<ParameterizedTypeReference<GithubSearchResponse>>()) } returns Mono.just(mockResponse)

        val service = GithubTrendingFetchService(webClient, rateLimiterRegistry, props)
        service.fetchPage("stars:>100", 1)

        verify { rateLimiterRegistry.rateLimiter("githubApi") }
    }

    test("fetchPage()는 여러 페이지 번호로 순차 호출할 수 있다") {
        val response1 = GithubSearchResponse(totalCount = 2, items = listOf(createItem("owner/repo1")))
        val response2 = GithubSearchResponse(totalCount = 2, items = listOf(createItem("owner/repo2")))

        every { responseSpec.bodyToMono(any<ParameterizedTypeReference<GithubSearchResponse>>()) } returnsMany
            listOf(Mono.just(response1), Mono.just(response2))

        val service = GithubTrendingFetchService(webClient, rateLimiterRegistry, props)

        val result1 = service.fetchPage("stars:>100", 1)
        val result2 = service.fetchPage("stars:>100", 2)

        result1.items[0].fullName shouldBe "owner/repo1"
        result2.items[0].fullName shouldBe "owner/repo2"
    }

    test("fetchPage()는 WebClient.get()을 통해 GET 요청을 보낸다") {
        val mockResponse = GithubSearchResponse(totalCount = 0, items = emptyList())
        every { responseSpec.bodyToMono(any<ParameterizedTypeReference<GithubSearchResponse>>()) } returns Mono.just(mockResponse)

        val service = GithubTrendingFetchService(webClient, rateLimiterRegistry, props)
        service.fetchPage("stars:>100", 1)

        verify(exactly = 1) { webClient.get() }
        verify(exactly = 1) { headersSpec.retrieve() }
    }
})

private fun createItem(
    fullName: String = "owner/repo",
    stargazersCount: Long = 1000L,
): GithubSearchResponse.Item = GithubSearchResponse.Item(
    id = fullName.hashCode().toLong(),
    name = fullName.substringAfter("/"),
    fullName = fullName,
    description = "Test description",
    htmlUrl = "https://github.com/$fullName",
    stargazersCount = stargazersCount,
    forksCount = 100L,
    language = "Kotlin",
    owner = GithubSearchResponse.Owner(
        login = fullName.substringBefore("/"),
        avatarUrl = null,
    ),
    topics = listOf("kotlin"),
    pushedAt = "2024-06-01T10:00:00Z",
)
