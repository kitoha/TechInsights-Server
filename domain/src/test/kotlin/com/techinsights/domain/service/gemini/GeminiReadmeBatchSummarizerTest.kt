package com.techinsights.domain.service.gemini

import com.google.genai.Client
import com.techinsights.domain.config.gemini.GeminiProperties
import com.techinsights.domain.dto.gemini.ArticleInput
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
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

    test("summarize()는 Gemini API 예외 시 모든 아이템을 실패로 반환한다") {
        every { circuitBreaker.executeCallable<Any>(any()) } throws RuntimeException("Gemini API down")

        val summarizer = GeminiReadmeBatchSummarizer(
            geminiClient, geminiProperties, promptBuilder,
            rateLimiterRegistry, circuitBreakerRegistry,
            ioDispatcher = Dispatchers.Unconfined,
        )

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
        // Gemini가 repo1만 응답하고 repo2는 누락한 상황 시뮬레이션
        // → circuitBreaker가 빈 iterator를 반환하면 parser가 아무것도 파싱하지 않음
        every { circuitBreaker.executeCallable<Any>(any()) } returns listOf<Any>().iterator()

        val summarizer = GeminiReadmeBatchSummarizer(
            geminiClient, geminiProperties, promptBuilder,
            rateLimiterRegistry, circuitBreakerRegistry,
            ioDispatcher = Dispatchers.Unconfined,
        )

        val items = listOf(
            ArticleInput("owner/repo1", "repo1", "readme1"),
        )

        val results = summarizer.summarize(items).toList()

        results shouldHaveSize 1
        results[0].id shouldBe "owner/repo1"
        results[0].success.shouldBeFalse()
    }

    test("summarize()는 빈 리스트에 대해 빈 Flow를 반환한다") {
        val summarizer = GeminiReadmeBatchSummarizer(
            geminiClient, geminiProperties, promptBuilder,
            rateLimiterRegistry, circuitBreakerRegistry,
            ioDispatcher = Dispatchers.Unconfined,
        )

        val results = summarizer.summarize(emptyList()).toList()

        results shouldHaveSize 0
    }
})
