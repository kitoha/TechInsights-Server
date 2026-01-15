package com.techinsights.domain.service.gemini

import com.google.genai.Client
import com.techinsights.domain.config.gemini.GeminiProperties
import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import com.techinsights.domain.enums.ErrorType
import com.techinsights.domain.enums.GeminiModelType
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList

class GeminiBatchArticleSummarizerTest : FunSpec({

    val geminiClient = mockk<Client>(relaxed = true)
    val geminiProperties = mockk<GeminiProperties>(relaxed = true)
    val promptBuilder = mockk<BatchPromptBuilder>(relaxed = true)
    val rateLimiterRegistry = mockk<RateLimiterRegistry>(relaxed = true)
    val circuitBreakerRegistry = mockk<CircuitBreakerRegistry>(relaxed = true)
    
    val rateLimiter = mockk<RateLimiter>(relaxed = true)
    val circuitBreaker = mockk<CircuitBreaker>(relaxed = true)

    lateinit var summarizer: GeminiBatchArticleSummarizer

    beforeTest {
        clearAllMocks()

        every { rateLimiterRegistry.rateLimiter(or("geminiBatchRpm", "geminiBatchRpd")) } returns rateLimiter
        every { circuitBreakerRegistry.circuitBreaker(any()) } returns circuitBreaker

        summarizer = GeminiBatchArticleSummarizer(
            geminiClient,
            geminiProperties,
            promptBuilder,
            rateLimiterRegistry,
            circuitBreakerRegistry
        )
    }

    test("summarizeBatch should return all results from the stream") {
        val articles = (1..3).map { ArticleInput("id-$it", "T", "C") }
        val spySummarizer = spyk(summarizer, recordPrivateCalls = true)

        val expectedResults = articles.map { SummaryResultWithId(it.id, true) }
        
        coEvery { 
            spySummarizer["callGeminiApi"](any<List<ArticleInput>>(), any<GeminiModelType>()) 
        } returns flowOf(*expectedResults.toTypedArray())

        val resultFlow = spySummarizer.summarizeBatch(articles, GeminiModelType.GEMINI_2_5_FLASH_LITE)
        val results = resultFlow.toList()

        results shouldHaveSize 3
        results.all { it.success } shouldBe true
    }

    test("summarizeBatch should mark missing articles as failed (Reconciliation)") {
        val articles = (1..3).map { ArticleInput("id-$it", "T", "C") }
        val spySummarizer = spyk(summarizer, recordPrivateCalls = true)

        // Only 2 results returned from stream, id-3 is missing
        val partialResults = articles.take(2).map { SummaryResultWithId(it.id, true) }
        
        coEvery { 
            spySummarizer["callGeminiApi"](any<List<ArticleInput>>(), any<GeminiModelType>()) 
        } returns flowOf(*partialResults.toTypedArray())

        val resultFlow = spySummarizer.summarizeBatch(articles, GeminiModelType.GEMINI_2_5_FLASH_LITE)
        val results = resultFlow.toList()

        results shouldHaveSize 3
        results.count { it.success } shouldBe 2
        
        val missingPost = results.find { it.id == "id-3" }
        missingPost?.success shouldBe false
        missingPost?.error shouldBe "Response missing from Gemini stream (possible parsing or model error)"
    }

    test("summarizeBatch should propagate specific error types to missing articles") {
        val articles = (1..3).map { ArticleInput("id-$it", "T", "C") }
        val spySummarizer = spyk(summarizer, recordPrivateCalls = true)

        val mixedResults = listOf(
            SummaryResultWithId("id-1", true),
            SummaryResultWithId("SYSTEM_ERROR_MARKER", false, error = "Safety blocked", errorType = ErrorType.SAFETY_BLOCKED)
        )
        
        coEvery { 
            spySummarizer["callGeminiApi"](any<List<ArticleInput>>(), any<GeminiModelType>()) 
        } returns flowOf(*mixedResults.toTypedArray())

        val resultFlow = spySummarizer.summarizeBatch(articles, GeminiModelType.GEMINI_2_5_FLASH_LITE)
        val results = resultFlow.toList()

        results shouldHaveSize 3
        results.find { it.id == "id-1" }?.success shouldBe true
        
        val blockedPost = results.find { it.id == "id-2" }
        blockedPost?.success shouldBe false
        blockedPost?.errorType shouldBe ErrorType.SAFETY_BLOCKED
        blockedPost?.error shouldBe "Safety blocked"
    }

    test("summarizeBatch should handle empty articles list") {
        val resultFlow = summarizer.summarizeBatch(emptyList(), GeminiModelType.GEMINI_2_5_FLASH_LITE)
        val results = resultFlow.toList()
        results shouldHaveSize 0
    }
})