package com.techinsights.domain.service.gemini

import com.google.genai.Client
import com.techinsights.domain.config.gemini.GeminiProperties
import com.techinsights.domain.dto.gemini.ArticleInput
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList

class GeminiReadmeBatchSummarizerTest : FunSpec({

    val geminiClient = mockk<Client>(relaxed = true)
    val geminiProperties = mockk<GeminiProperties>()
    val promptBuilder = mockk<GithubReadmePromptBuilder>()
    val rateLimiterRegistry = mockk<RateLimiterRegistry>()
    val circuitBreakerRegistry = mockk<CircuitBreakerRegistry>()
    val rpmLimiter = mockk<RateLimiter>(relaxed = true)
    val rpdLimiter = mockk<RateLimiter>(relaxed = true)
    val circuitBreaker = mockk<CircuitBreaker>()

    beforeTest {
        clearAllMocks()
        every { rateLimiterRegistry.rateLimiter("geminiReadmeRpm") } returns rpmLimiter
        every { rateLimiterRegistry.rateLimiter("geminiReadmeRpd") } returns rpdLimiter
        every { circuitBreakerRegistry.circuitBreaker("geminiBatch") } returns circuitBreaker
        every { geminiProperties.maxOutputTokens } returns 8192
        every { promptBuilder.buildPrompt(any()) } returns "test prompt"
        every { promptBuilder.buildSchema() } returns """{"type":"object"}"""
    }

    test("summarize()는 빈 리스트에 대해 빈 Flow를 반환한다") {
        val summarizer = buildSummarizer(geminiClient, geminiProperties, promptBuilder, rateLimiterRegistry, circuitBreakerRegistry)

        val results = summarizer.summarize(emptyList()).toList()

        results shouldHaveSize 0
    }

    test("summarize()는 Gemini API 예외 시 모든 아이템을 실패로 반환한다") {
        every { circuitBreaker.executeCallable<Any>(any()) } throws RuntimeException("Gemini API down")

        val summarizer = buildSummarizer(geminiClient, geminiProperties, promptBuilder, rateLimiterRegistry, circuitBreakerRegistry)
        val items = listOf(
            ArticleInput("owner/repo1", "repo1", "readme1"),
            ArticleInput("owner/repo2", "repo2", "readme2"),
        )

        val results = summarizer.summarize(items).toList()

        results shouldHaveSize 2
        results.all { !it.success } shouldBe true
        results.map { it.id }.toSet() shouldBe setOf("owner/repo1", "owner/repo2")
    }

    test("summarize()는 Gemini 응답에서 누락된 ID를 실패로 보고한다") {
        every { circuitBreaker.executeCallable<Any>(any()) } returns listOf<Any>().iterator()

        val summarizer = buildSummarizer(geminiClient, geminiProperties, promptBuilder, rateLimiterRegistry, circuitBreakerRegistry)
        val items = listOf(ArticleInput("owner/repo1", "repo1", "readme1"))

        val results = summarizer.summarize(items).toList()

        results shouldHaveSize 1
        results[0].id shouldBe "owner/repo1"
        results[0].success.shouldBeFalse()
    }

    test("summarize()는 RPM과 RPD rate limiter를 모두 획득한다") {
        every { circuitBreaker.executeCallable<Any>(any()) } returns listOf<Any>().iterator()

        val summarizer = buildSummarizer(geminiClient, geminiProperties, promptBuilder, rateLimiterRegistry, circuitBreakerRegistry)
        val items = listOf(ArticleInput("owner/repo1", "repo1", "readme1"))

        summarizer.summarize(items).toList()

        verify(exactly = 1) { rpmLimiter.acquirePermission() }
        verify(exactly = 1) { rpdLimiter.acquirePermission() }
    }

    test("summarize()는 아이템이 여러 개여도 Gemini 호출은 1회만 한다") {
        every { circuitBreaker.executeCallable<Any>(any()) } returns listOf<Any>().iterator()

        val summarizer = buildSummarizer(geminiClient, geminiProperties, promptBuilder, rateLimiterRegistry, circuitBreakerRegistry)
        val items = listOf(
            ArticleInput("owner/repo1", "repo1", "readme1"),
            ArticleInput("owner/repo2", "repo2", "readme2"),
            ArticleInput("owner/repo3", "repo3", "readme3"),
        )

        summarizer.summarize(items).toList()

        verify(exactly = 1) { rpmLimiter.acquirePermission() }
        verify(exactly = 1) { rpdLimiter.acquirePermission() }
        verify(exactly = 1) { circuitBreaker.executeCallable<Any>(any()) }
    }

    test("summarize()는 Circuit Breaker를 통해 Gemini API를 호출한다") {
        every { circuitBreaker.executeCallable<Any>(any()) } returns listOf<Any>().iterator()

        val summarizer = buildSummarizer(geminiClient, geminiProperties, promptBuilder, rateLimiterRegistry, circuitBreakerRegistry)
        val items = listOf(ArticleInput("owner/repo1", "repo1", "readme1"))

        summarizer.summarize(items).toList()

        verify(exactly = 1) { circuitBreaker.executeCallable<Any>(any()) }
    }

    test("summarize()는 CircuitBreaker OPEN 상태에서 모든 아이템을 실패로 반환한다") {
        every { circuitBreaker.name } returns "geminiBatch"
        every { circuitBreaker.state } returns CircuitBreaker.State.OPEN
        every { circuitBreaker.circuitBreakerConfig } returns CircuitBreakerConfig.ofDefaults()
        val openException = CallNotPermittedException.createCallNotPermittedException(circuitBreaker)
        every { circuitBreaker.executeCallable<Any>(any()) } throws openException

        val summarizer = buildSummarizer(geminiClient, geminiProperties, promptBuilder, rateLimiterRegistry, circuitBreakerRegistry)
        val items = listOf(
            ArticleInput("owner/repo1", "repo1", "readme1"),
            ArticleInput("owner/repo2", "repo2", "readme2"),
        )

        val results = summarizer.summarize(items).toList()

        results shouldHaveSize 2
        results.all { !it.success }.shouldBeTrue()
        results.all { !it.error.isNullOrBlank() }.shouldBeTrue()
    }

    test("summarize()는 예외 발생 시 error 메시지가 비어있지 않다") {
        every { circuitBreaker.executeCallable<Any>(any()) } throws RuntimeException("timeout")

        val summarizer = buildSummarizer(geminiClient, geminiProperties, promptBuilder, rateLimiterRegistry, circuitBreakerRegistry)
        val items = listOf(ArticleInput("owner/repo1", "repo1", "readme1"))

        val results = summarizer.summarize(items).toList()

        results[0].success.shouldBeFalse()
        results[0].error.shouldNotBeEmpty()
    }

    test("summarize()는 Gemini 빈 응답 시 누락 아이템 error 메시지가 비어있지 않다") {
        every { circuitBreaker.executeCallable<Any>(any()) } returns listOf<Any>().iterator()

        val summarizer = buildSummarizer(geminiClient, geminiProperties, promptBuilder, rateLimiterRegistry, circuitBreakerRegistry)
        val items = listOf(ArticleInput("owner/repo1", "repo1", "readme1"))

        val results = summarizer.summarize(items).toList()

        results[0].success.shouldBeFalse()
        results[0].error.shouldNotBeEmpty()
    }
})

private fun buildSummarizer(
    geminiClient: Client,
    geminiProperties: GeminiProperties,
    promptBuilder: GithubReadmePromptBuilder,
    rateLimiterRegistry: RateLimiterRegistry,
    circuitBreakerRegistry: CircuitBreakerRegistry,
) = GeminiReadmeBatchSummarizer(
    geminiClient = geminiClient,
    geminiProperties = geminiProperties,
    promptBuilder = promptBuilder,
    rateLimiterRegistry = rateLimiterRegistry,
    circuitBreakerRegistry = circuitBreakerRegistry,
    ioDispatcher = Dispatchers.Unconfined,
)
