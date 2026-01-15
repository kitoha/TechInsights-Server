package com.techinsights.domain.service.gemini

import com.google.genai.Client
import com.techinsights.domain.config.gemini.GeminiProperties
import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.BatchSummaryResponse
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.exception.JsonTruncationException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*

class GeminiBatchArticleSummarizerTest : FunSpec({

    val geminiClient = mockk<Client>(relaxed = true)
    val geminiProperties = mockk<GeminiProperties>(relaxed = true)
    val promptBuilder = mockk<BatchPromptBuilder>(relaxed = true)
    val responseParser = mockk<BatchResponseParser>(relaxed = true)
    val responseProcessor = mockk<BatchResponseProcessor>(relaxed = true)
    val rateLimiterRegistry = mockk<RateLimiterRegistry>(relaxed = true)
    val circuitBreakerRegistry = mockk<CircuitBreakerRegistry>(relaxed = true)
    
    val rateLimiter = mockk<RateLimiter>(relaxed = true)
    val circuitBreaker = mockk<CircuitBreaker>(relaxed = true)

    lateinit var summarizer: GeminiBatchArticleSummarizer

    beforeTest {
        clearAllMocks()
        
        every { rateLimiterRegistry.rateLimiter(any()) } returns rateLimiter
        every { circuitBreakerRegistry.circuitBreaker(any()) } returns circuitBreaker
        every { responseProcessor.process(any(), any(), any()) } answers { firstArg() }

        summarizer = GeminiBatchArticleSummarizer(
            geminiClient,
            geminiProperties,
            promptBuilder,
            responseParser,
            responseProcessor,
            rateLimiterRegistry,
            circuitBreakerRegistry
        )
    }

    test("summarizeBatch should fallback to binary search on JSON truncation error") {
        val articles = (1..4).map { ArticleInput("id-$it", "T", "C") }
        val spySummarizer = spyk(summarizer, recordPrivateCalls = true)

        coEvery { 
            spySummarizer["callGeminiApi"](any<List<ArticleInput>>(), any<GeminiModelType>()) 
        } returnsMany listOf("trunc", "ok1", "ok2")

        every { responseParser.parse("trunc", any()) } throws JsonTruncationException("Truncated")
        every { responseParser.parse("ok1", any()) } returns BatchSummaryResponse(articles.take(2).map { SummaryResultWithId(it.id, true) })
        every { responseParser.parse("ok2", any()) } returns BatchSummaryResponse(articles.drop(2).map { SummaryResultWithId(it.id, true) })

        val result = spySummarizer.summarizeBatch(articles, GeminiModelType.GEMINI_3_FLASH)

        result.results shouldHaveSize 4
        result.results.all { it.success } shouldBe true
        
        coVerify(atLeast = 1) { spySummarizer["callGeminiApi"](any<List<ArticleInput>>(), any<GeminiModelType>()) }
    }

    test("summarizeBatch should handle mixed success in fallback") {
        val articles = (1..4).map { ArticleInput("id-$it", "T", "C") }
        val spySummarizer = spyk(summarizer, recordPrivateCalls = true)

        coEvery { 
            spySummarizer["callGeminiApi"](any<List<ArticleInput>>(), any<GeminiModelType>()) 
        } returnsMany listOf("fFull", "sH1", "fH2", "s3", "f4")

        every { responseParser.parse("fFull", any()) } throws JsonTruncationException("Truncated")
        every { responseParser.parse("sH1", any()) } returns BatchSummaryResponse(listOf(SummaryResultWithId("id-1", true), SummaryResultWithId("id-2", true)))
        every { responseParser.parse("fH2", any()) } throws JsonTruncationException("Truncated")
        every { responseParser.parse("s3", any()) } returns BatchSummaryResponse(listOf(SummaryResultWithId("id-3", true)))
        
        every { responseParser.parse("f4", any()) } throws RuntimeException("API Error")
        every { responseParser.createFailureResponse(match { it[0].id == "id-4" }, any()) } returns BatchSummaryResponse(listOf(SummaryResultWithId("id-4", false, error = "API Error")))

        val result = spySummarizer.summarizeBatch(articles, GeminiModelType.GEMINI_3_FLASH)

        result.results shouldHaveSize 4
        result.results.find { it.id == "id-4" }?.success shouldBe false
    }

    test("summarizeBatch should handle non-splittable error (UNKNOWN)") {
        val articles = listOf(ArticleInput("1", "T", "C"), ArticleInput("2", "T", "C"))
        val spySummarizer = spyk(summarizer, recordPrivateCalls = true)

        coEvery { 
            spySummarizer["callGeminiApi"](any<List<ArticleInput>>(), any<GeminiModelType>()) 
        } throws RuntimeException("Something went wrong")

        every { responseParser.createFailureResponse(any(), any()) } answers {
            val inputs = firstArg<List<ArticleInput>>()
            BatchSummaryResponse(inputs.map { SummaryResultWithId(it.id, false, error = "Something went wrong") })
        }

        val result = spySummarizer.summarizeBatch(articles, GeminiModelType.GEMINI_3_FLASH)

        result.results shouldHaveSize 2
        result.results.all { !it.success } shouldBe true
    }
})
