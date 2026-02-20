package com.techinsights.batch.summary.service

import com.techinsights.batch.summary.config.props.BatchProcessingProperties
import com.techinsights.batch.summary.dto.BatchRequest
import com.techinsights.batch.summary.dto.BatchResult
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.ErrorType
import com.techinsights.domain.service.gemini.BatchArticleSummarizer
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import java.time.LocalDateTime

class AsyncBatchSummarizationServiceTest : FunSpec({

    val batchSummarizer = mockk<BatchArticleSummarizer>()
    val resultProcessor = mockk<BatchResultProcessor>()
    val retryPolicy = mockk<BatchRetryPolicy>()
    val errorClassifier = mockk<ErrorClassifier>()
    val circuitBreakerRegistry = mockk<CircuitBreakerRegistry>()
    val circuitBreaker = mockk<CircuitBreaker>()

    val properties = BatchProcessingProperties(
        concurrencyLimit = 2,
        timeoutMs = 1000L,
        maxRetryAttempts = 2,
        retry = BatchProcessingProperties.RetryConfig(1, 1, 1)
    )

    lateinit var service: AsyncBatchSummarizationService

    beforeTest {
        clearAllMocks()
        every { circuitBreakerRegistry.circuitBreaker("geminiBatch") } returns circuitBreaker
        every { circuitBreaker.state } returns CircuitBreaker.State.CLOSED
        
        service = AsyncBatchSummarizationService(
            batchSummarizer,
            resultProcessor,
            retryPolicy,
            errorClassifier,
            properties,
            circuitBreakerRegistry
        )
    }

    test("processBatchesAsync should process successful batch") {
        runTest {
            val post = createMockPost("1")
            val request = BatchRequest("req-1", listOf(post), 100, 0)
            val expectedResult = mockk<BatchResult>()

            coEvery { batchSummarizer.summarizeBatch(any(), any()) } returns emptyFlow()
            every { resultProcessor.processBatchResponse(request, any(), any()) } returns expectedResult

            val results = service.processBatchesAsync(listOf(request))

            results.size shouldBe 1
            results[0] shouldBe expectedResult
        }
    }

    test("processBatchesAsync should retry on error and eventually succeed") {
        runTest {
            val post = createMockPost("1")
            val request = BatchRequest("req-1", listOf(post), 100, 0)
            val expectedResult = mockk<BatchResult>()

            coEvery { batchSummarizer.summarizeBatch(any(), any()) } throws RuntimeException("API Error") andThen emptyFlow()
            
            every { errorClassifier.classify(any()) } returns ErrorType.API_ERROR
            every { retryPolicy.shouldRetry(ErrorType.API_ERROR, any()) } returnsMany listOf(true, false)
            every { retryPolicy.calculateBackoffDelay(ErrorType.API_ERROR, any()) } returns 1L
            
            every { resultProcessor.processBatchResponse(request, any(), any()) } returns expectedResult

            val results = service.processBatchesAsync(listOf(request))

            results.size shouldBe 1
            results[0] shouldBe expectedResult
            
            coVerify(atLeast = 2) { batchSummarizer.summarizeBatch(any(), any()) }
        }
    }

    test("processBatchesAsync should retry max attempts then fail") {
        runTest {
            val post = createMockPost("1")
            val request = BatchRequest("req-1", listOf(post), 100, 0)
            val failureResult = mockk<BatchResult>()

            coEvery { batchSummarizer.summarizeBatch(any(), any()) } throws RuntimeException("API Error")
            
            every { errorClassifier.classify(any()) } returns ErrorType.API_ERROR
            every { retryPolicy.shouldRetry(ErrorType.API_ERROR, 0) } returns true
            every { retryPolicy.calculateBackoffDelay(ErrorType.API_ERROR, 0) } returns 1L
            every { retryPolicy.shouldRetry(ErrorType.API_ERROR, 1) } returns false
            
            every { resultProcessor.createFailureResult(request, any(), ErrorType.API_ERROR) } returns failureResult

            val results = service.processBatchesAsync(listOf(request))

            results.size shouldBe 1
            results[0] shouldBe failureResult
        }
    }

    test("processBatchesAsync should handle Circuit Breaker OPEN state") {
        runTest {
            val post = createMockPost("1")
            val request = BatchRequest("req-1", listOf(post), 100, 0)
            val failureResult = mockk<BatchResult>()

            every { circuitBreaker.state } returns CircuitBreaker.State.OPEN
            every { resultProcessor.createFailureResult(request, any(), ErrorType.API_ERROR) } returns failureResult

            val results = service.processBatchesAsync(listOf(request))

            results.size shouldBe 1
            results[0] shouldBe failureResult
        }
    }
})

private fun createMockPost(id: String): PostDto {
    return PostDto(
        id = id,
        title = "Title",
        content = "Content",
        url = "Url",
        publishedAt = LocalDateTime.now(),
        company = mockk(relaxed = true),
        isSummary = false,
        preview = null,
        categories = emptySet(),
        isEmbedding = false
    )
}