package com.techinsights.batch.service

import com.techinsights.batch.dto.*
import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.enums.Category
import com.techinsights.domain.enums.ErrorType
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.service.gemini.BatchArticleSummarizer
import com.techinsights.domain.service.gemini.BatchSummaryValidator
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Semaphore
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

@Service
class AsyncBatchSummarizationService(
    private val batchSummarizer: BatchArticleSummarizer,
    private val validator: BatchSummaryValidator,
    circuitBreakerRegistry: CircuitBreakerRegistry,
    private val ioDispatcher: CoroutineContext = Dispatchers.IO
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker("geminiBatch")

    private val concurrencyLimit = Semaphore(5)

    suspend fun processBatchesAsync(batches: List<BatchRequest>): List<BatchResult> = coroutineScope {
        log.info("Processing ${batches.size} batches with total ${batches.sumOf { it.posts.size }} posts")

        val sortedBatches = batches.sortedByDescending { it.priority }

        sortedBatches.map { batch ->
            async(ioDispatcher) {
                processSingleBatchWithRetry(batch)
            }
        }.awaitAll()
    }

    private suspend fun processSingleBatchWithRetry(
        request: BatchRequest,
        retryCount: Int = 0
    ): BatchResult {
        val startTime = System.currentTimeMillis()

        if (circuitBreaker.state == CircuitBreaker.State.OPEN) {
            log.warn("Circuit breaker is OPEN, skipping batch ${request.id}")
            return createFailureResult(
                request,
                "Circuit breaker open - API unavailable",
                ErrorType.API_ERROR
            )
        }

        try {
            concurrencyLimit.acquire()

            return withTimeout(60_000) {
                processSingleBatch(request)
            }

        } catch (e: TimeoutCancellationException) {
            log.error("Batch ${request.id} timed out after 60s", e)

            if (retryCount < 2) {
                delay(2000L * (retryCount + 1))
                return processSingleBatchWithRetry(request, retryCount + 1)
            }

            return createFailureResult(request, "Timeout after retries", ErrorType.TIMEOUT)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error("Batch ${request.id} failed", e)

            val errorType = classifyError(e)
            val retryable = errorType in setOf(ErrorType.TIMEOUT, ErrorType.RATE_LIMIT)

            if (retryable && retryCount < 2) {
                delay(5000L * (retryCount + 1))
                return processSingleBatchWithRetry(request, retryCount + 1)
            }

            return createFailureResult(request, e.message ?: "Unknown error", errorType)

        } finally {
            concurrencyLimit.release()

            val duration = System.currentTimeMillis() - startTime
            log.info("Batch ${request.id} completed in ${duration}ms")
        }
    }

    private suspend fun processSingleBatch(request: BatchRequest): BatchResult {
        val startTime = System.currentTimeMillis()

        val inputs = request.posts.map {
            ArticleInput(
                id = it.id,
                title = it.title,
                content = it.content
            )
        }

        val batchResponse = batchSummarizer.summarizeBatch(
            inputs,
            GeminiModelType.GEMINI_2_5_FLASH_LITE
        )

        val successes = mutableListOf<com.techinsights.domain.dto.post.PostDto>()
        val failures = mutableListOf<BatchFailure>()

        for (post in request.posts) {
            val result = batchResponse.results.find { it.id == post.id }

            when {
                result == null -> {
                    failures.add(BatchFailure(
                        post = post,
                        reason = "No result returned for this post",
                        retryable = false,
                        errorType = ErrorType.VALIDATION_ERROR
                    ))
                }

                !result.success -> {
                    failures.add(BatchFailure(
                        post = post,
                        reason = result.error ?: "Unknown error",
                        retryable = true,
                        errorType = ErrorType.VALIDATION_ERROR
                    ))
                }

                else -> {
                    val validation = validator.validate(
                        ArticleInput(post.id, post.title, post.content),
                        result,
                        Category.entries.map { it.name }.toSet()
                    )

                    if (!validation.isValid) {
                        failures.add(BatchFailure(
                            post = post,
                            reason = validation.errors.joinToString(", "),
                            retryable = true,
                            errorType = ErrorType.VALIDATION_ERROR
                        ))
                    } else {
                        successes.add(post.copy(
                            content = result.summary ?: post.content,
                            preview = result.preview,
                            categories = result.categories
                                ?.mapNotNull { runCatching { Category.valueOf(it) }.getOrNull() }
                                ?.toSet()
                                ?: emptySet(),
                            isSummary = true
                        ))
                    }
                }
            }
        }

        val duration = System.currentTimeMillis() - startTime

        val metrics = BatchMetrics(
            totalItems = request.posts.size,
            successCount = successes.size,
            failureCount = failures.size,
            apiCallCount = 1,
            tokensUsed = request.estimatedTokens,
            durationMs = duration
        )

        return BatchResult(
            requestId = request.id,
            successes = successes,
            failures = failures,
            metrics = metrics
        )
    }

    private fun createFailureResult(
        request: BatchRequest,
        reason: String,
        errorType: ErrorType
    ): BatchResult {
        val failures = request.posts.map { post ->
            BatchFailure(
                post = post,
                reason = reason,
                retryable = errorType in setOf(ErrorType.TIMEOUT, ErrorType.RATE_LIMIT),
                errorType = errorType
            )
        }

        return BatchResult(
            requestId = request.id,
            successes = emptyList(),
            failures = failures,
            metrics = BatchMetrics(
                totalItems = request.posts.size,
                successCount = 0,
                failureCount = request.posts.size,
                apiCallCount = 0,
                tokensUsed = 0,
                durationMs = 0
            )
        )
    }

    private fun classifyError(e: Exception): ErrorType {
        return when {
            e.message?.contains("rate limit", ignoreCase = true) == true -> ErrorType.RATE_LIMIT
            e.message?.contains("timeout", ignoreCase = true) == true -> ErrorType.TIMEOUT
            e is TimeoutCancellationException -> ErrorType.TIMEOUT
            else -> ErrorType.API_ERROR
        }
    }
}
