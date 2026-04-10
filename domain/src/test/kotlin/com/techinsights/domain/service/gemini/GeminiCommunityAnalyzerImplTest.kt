package com.techinsights.domain.service.gemini

import com.google.genai.Client
import com.techinsights.domain.config.gemini.GeminiProperties
import com.techinsights.domain.dto.community.CommunityAnalysisInput
import com.techinsights.domain.dto.community.CommunityPost
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

class GeminiCommunityAnalyzerImplTest : FunSpec({

    val geminiClient = mockk<Client>(relaxed = true)
    val geminiProperties = mockk<GeminiProperties>()
    val promptBuilder = mockk<CommunityBuzzPromptBuilder>()
    val rateLimiterRegistry = mockk<RateLimiterRegistry>()
    val circuitBreakerRegistry = mockk<CircuitBreakerRegistry>()
    val rpmLimiter = mockk<RateLimiter>(relaxed = true)
    val rpdLimiter = mockk<RateLimiter>(relaxed = true)
    val circuitBreaker = mockk<CircuitBreaker>()

    beforeTest {
        clearAllMocks()
        every { rateLimiterRegistry.rateLimiter("geminiCommunityRpm") } returns rpmLimiter
        every { rateLimiterRegistry.rateLimiter("geminiCommunityRpd") } returns rpdLimiter
        every { circuitBreakerRegistry.circuitBreaker("communityInsight") } returns circuitBreaker
        every { geminiProperties.maxTokensPerRequest } returns 100_000
        every { geminiProperties.maxOutputTokens } returns 65_536
        every { geminiProperties.inputTokensPerItem } returns 500
        every { geminiProperties.outputTokensPerItem } returns 200
        every { promptBuilder.buildPrompt(any()) } returns "test prompt"
        every { promptBuilder.buildSchema() } returns """{"type":"object"}"""
    }

    fun buildAnalyzer() = GeminiCommunityAnalyzerImpl(
        geminiClient = geminiClient,
        geminiProperties = geminiProperties,
        promptBuilder = promptBuilder,
        rateLimiterRegistry = rateLimiterRegistry,
        circuitBreakerRegistry = circuitBreakerRegistry,
        ioDispatcher = Dispatchers.Unconfined,
    )

    fun input(repoFullName: String = "owner/repo") = CommunityAnalysisInput(
        repoFullName = repoFullName,
        repoName = repoFullName.substringAfter("/"),
        hnPosts = listOf(CommunityPost("hn", "Post", 10, 3, "user", "https://hn.com")),
        redditPosts = emptyList(),
    )

    test("analyze()는 빈 리스트에 대해 빈 Flow를 반환한다") {
        val results = buildAnalyzer().analyze(emptyList()).toList()

        results shouldHaveSize 0
    }

    test("analyze()는 Gemini API 예외 시 모든 아이템을 실패로 반환한다") {
        every { circuitBreaker.executeCallable<Any>(any()) } throws RuntimeException("Gemini API down")

        val results = buildAnalyzer().analyze(listOf(input("owner/repo1"), input("owner/repo2"))).toList()

        results shouldHaveSize 2
        results.all { !it.success }.shouldBeTrue()
        results.map { it.id }.toSet() shouldBe setOf("owner/repo1", "owner/repo2")
    }

    test("analyze()는 Gemini 응답에서 누락된 ID를 실패로 반환한다") {
        every { circuitBreaker.executeCallable<Any>(any()) } returns listOf<Any>().iterator()

        val results = buildAnalyzer().analyze(listOf(input("owner/repo1"))).toList()

        results shouldHaveSize 1
        results[0].id shouldBe "owner/repo1"
        results[0].success.shouldBeFalse()
    }

    test("analyze()는 RPM과 RPD rate limiter를 모두 획득한다") {
        every { circuitBreaker.executeCallable<Any>(any()) } returns listOf<Any>().iterator()

        buildAnalyzer().analyze(listOf(input())).toList()

        verify(exactly = 1) { rpmLimiter.acquirePermission() }
        verify(exactly = 1) { rpdLimiter.acquirePermission() }
    }

    test("analyze()는 아이템이 여러 개여도 Gemini 호출은 1회만 한다") {
        every { circuitBreaker.executeCallable<Any>(any()) } returns listOf<Any>().iterator()

        buildAnalyzer().analyze(listOf(input("owner/repo1"), input("owner/repo2"), input("owner/repo3"))).toList()

        verify(exactly = 1) { rpmLimiter.acquirePermission() }
        verify(exactly = 1) { rpdLimiter.acquirePermission() }
        verify(exactly = 1) { circuitBreaker.executeCallable<Any>(any()) }
    }

    test("analyze()는 Circuit Breaker를 통해 Gemini API를 호출한다") {
        every { circuitBreaker.executeCallable<Any>(any()) } returns listOf<Any>().iterator()

        buildAnalyzer().analyze(listOf(input())).toList()

        verify(exactly = 1) { circuitBreaker.executeCallable<Any>(any()) }
    }

    test("analyze()는 CircuitBreaker OPEN 상태에서 모든 아이템을 실패로 반환한다") {
        every { circuitBreaker.name } returns "communityInsight"
        every { circuitBreaker.state } returns CircuitBreaker.State.OPEN
        every { circuitBreaker.circuitBreakerConfig } returns CircuitBreakerConfig.ofDefaults()
        val openException = CallNotPermittedException.createCallNotPermittedException(circuitBreaker)
        every { circuitBreaker.executeCallable<Any>(any()) } throws openException

        val results = buildAnalyzer().analyze(listOf(input("owner/repo1"), input("owner/repo2"))).toList()

        results shouldHaveSize 2
        results.all { !it.success }.shouldBeTrue()
        results.all { !it.error.isNullOrBlank() }.shouldBeTrue()
    }

    test("analyze()는 예외 발생 시 error 메시지가 비어있지 않다") {
        every { circuitBreaker.executeCallable<Any>(any()) } throws RuntimeException("timeout")

        val results = buildAnalyzer().analyze(listOf(input())).toList()

        results[0].success.shouldBeFalse()
        results[0].error.shouldNotBeEmpty()
    }

    test("analyze()는 Gemini 빈 응답 시 누락 아이템의 error 메시지가 비어있지 않다") {
        every { circuitBreaker.executeCallable<Any>(any()) } returns listOf<Any>().iterator()

        val results = buildAnalyzer().analyze(listOf(input())).toList()

        results[0].success.shouldBeFalse()
        results[0].error.shouldNotBeEmpty()
    }

    test("calculateOutputTokens — 소수 배치에서 항목 목표가 잔여 예산보다 작으면 항목 목표를 사용한다") {
        buildAnalyzer().calculateOutputTokens(10) shouldBe 2_000
    }

    test("calculateOutputTokens — 입력 추정이 예산 초과 시 최소값 1024를 반환한다") {
        buildAnalyzer().calculateOutputTokens(300) shouldBe 1_024
    }
})
