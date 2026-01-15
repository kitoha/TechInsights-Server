package com.techinsights.batch.service

import com.techinsights.batch.config.props.BatchProcessingProperties
import com.techinsights.batch.dto.BatchRequest
import com.techinsights.batch.dto.BatchResult
import com.techinsights.domain.dto.gemini.BatchSummaryResponse
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.ErrorType
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.service.gemini.BatchArticleSummarizer
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.TimeoutCancellationException
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
        retry = BatchProcessingProperties.RetryConfig(10, 10, 10)
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
            // Given
            val post = createMockPost("1")
            val request = BatchRequest("req-1", listOf(post), 100, 0)
            val summaryResponse = BatchSummaryResponse(emptyList())
            val expectedResult = mockk<BatchResult>()

            coEvery { batchSummarizer.summarizeBatch(any(), any()) } returns summaryResponse
            every { resultProcessor.processBatchResponse(request, summaryResponse, any()) } returns expectedResult

            // When
            val results = service.processBatchesAsync(listOf(request))

            // Then
            results.size shouldBe 1
            results[0] shouldBe expectedResult
            
            coVerify(exactly = 1) { 
                batchSummarizer.summarizeBatch(any(), GeminiModelType.GEMINI_2_5_FLASH_LITE) 
            }
        }
    }

    test("processBatchesAsync should retry on timeout and eventually succeed") {
        runTest {
            // Given
            val post = createMockPost("1")
            val request = BatchRequest("req-1", listOf(post), 100, 0)
            val summaryResponse = BatchSummaryResponse(emptyList())
            val expectedResult = mockk<BatchResult>()

            // 1st attempt: Timeout, 2nd attempt: Success
            val timeoutException = mockk<TimeoutCancellationException>()
            coEvery { batchSummarizer.summarizeBatch(any(), any()) } throws timeoutException andThen summaryResponse
            
            every { errorClassifier.classify(any()) } returns ErrorType.TIMEOUT
            every { retryPolicy.shouldRetry(ErrorType.TIMEOUT, 0) } returns true
            every { retryPolicy.calculateBackoffDelay(ErrorType.TIMEOUT, 0) } returns 1L
            
            every { resultProcessor.processBatchResponse(request, summaryResponse, any()) } returns expectedResult

            // When
            val results = service.processBatchesAsync(listOf(request))

            // Then
            results.size shouldBe 1
            results[0] shouldBe expectedResult
            
            coVerify(exactly = 2) { batchSummarizer.summarizeBatch(any(), any()) }
        }
    }

    test("processBatchesAsync should retry max attempts then fail") {
        runTest {
            // Given
            val post = createMockPost("1")
            val request = BatchRequest("req-1", listOf(post), 100, 0)
            val failureResult = mockk<BatchResult>()

            coEvery { batchSummarizer.summarizeBatch(any(), any()) } throws RuntimeException("API Error")
            
            every { errorClassifier.classify(any()) } returns ErrorType.API_ERROR
            every { retryPolicy.shouldRetry(ErrorType.API_ERROR, 0) } returns true
            every { retryPolicy.calculateBackoffDelay(ErrorType.API_ERROR, 0) } returns 1L
            every { retryPolicy.shouldRetry(ErrorType.API_ERROR, 1) } returns false
            
            every { resultProcessor.createFailureResult(request, any(), ErrorType.API_ERROR) } returns failureResult

            // When
            val results = service.processBatchesAsync(listOf(request))

            // Then
            results.size shouldBe 1
            results[0] shouldBe failureResult
            
            coVerify(exactly = 2) { batchSummarizer.summarizeBatch(any(), any()) }
        }
    }

    test("processBatchesAsync should handle Circuit Breaker OPEN state") {
        runTest {
            // Given
            val post = createMockPost("1")
            val request = BatchRequest("req-1", listOf(post), 100, 0)
            val failureResult = mockk<BatchResult>()

            every { circuitBreaker.state } returns CircuitBreaker.State.OPEN
            every { resultProcessor.createFailureResult(request, any(), ErrorType.API_ERROR) } returns failureResult

            // When
            val results = service.processBatchesAsync(listOf(request))

            // Then
            results.size shouldBe 1
            results[0] shouldBe failureResult
            
            coVerify(exactly = 0) { batchSummarizer.summarizeBatch(any(), any()) }
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
        company = mockk(),
        isSummary = false,
        preview = null,
        categories = emptySet(),
        isEmbedding = false
    )
}
