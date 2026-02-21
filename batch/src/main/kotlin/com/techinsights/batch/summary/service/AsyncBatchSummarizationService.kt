package com.techinsights.batch.summary.service

import com.techinsights.batch.summary.config.props.BatchProcessingProperties
import com.techinsights.batch.summary.dto.BatchRequest
import com.techinsights.batch.summary.dto.BatchResult
import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.BatchSummaryResponse
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.service.gemini.BatchArticleSummarizer
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
    private val resultProcessor: BatchResultProcessor,
    private val retryPolicy: BatchRetryPolicy,
    private val errorClassifier: ErrorClassifier,
    private val properties: BatchProcessingProperties,
    circuitBreakerRegistry: CircuitBreakerRegistry,
    private val ioDispatcher: CoroutineContext = Dispatchers.IO
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker("geminiBatch")
    private val concurrencyLimit: Semaphore by lazy { Semaphore(properties.concurrencyLimit) }

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

        if (isCircuitBreakerOpen()) {
            return handleCircuitBreakerOpen(request)
        }

        return try {
            concurrencyLimit.acquire()
            executeBatchWithTimeout(request)
        } catch (e: TimeoutCancellationException) {
            handleTimeout(request, retryCount, e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleException(request, retryCount, e)
        } finally {
            concurrencyLimit.release()
            logBatchCompletion(request.id, startTime)
        }
    }

    private fun isCircuitBreakerOpen(): Boolean {
        return circuitBreaker.state == CircuitBreaker.State.OPEN
    }

    private fun handleCircuitBreakerOpen(request: BatchRequest): BatchResult {
        log.warn("Circuit breaker is OPEN, skipping batch ${request.id}")
        return resultProcessor.createFailureResult(
            request,
            "Circuit breaker open - API unavailable",
            com.techinsights.domain.enums.ErrorType.API_ERROR
        )
    }

    private suspend fun executeBatchWithTimeout(request: BatchRequest): BatchResult {
        return withTimeout(properties.timeoutMs) {
            processSingleBatch(request)
        }
    }

    private suspend fun handleTimeout(
        request: BatchRequest,
        retryCount: Int,
        exception: TimeoutCancellationException
    ): BatchResult {
        log.error("Batch ${request.id} timed out after ${properties.timeoutMs}ms", exception)

        val errorType = errorClassifier.classify(exception)
        
        if (retryPolicy.shouldRetry(errorType, retryCount)) {
            val delay = retryPolicy.calculateBackoffDelay(errorType, retryCount)
            delay(delay)
            log.warn("Batch ${request.id} for posts [${request.posts.map { it.id }.joinToString()}] timed out. Retrying after ${delay}ms (attempt ${retryCount + 1}/${properties.maxRetryAttempts})")
            return processSingleBatchWithRetry(request, retryCount + 1)
        }

        log.error("Batch ${request.id} for posts [${request.posts.map { it.id }.joinToString()}] failed permanently due to timeout after ${properties.maxRetryAttempts} retries.", exception)
        return resultProcessor.createFailureResult(
            request,
            "Timeout after retries",
            errorType
        )
    }

    private suspend fun handleException(
        request: BatchRequest,
        retryCount: Int,
        exception: Exception
    ): BatchResult {
        log.error("Batch ${request.id} for posts [${request.posts.map { it.id }.joinToString()}] failed.", exception)

        val errorType = errorClassifier.classify(exception)

        if (retryPolicy.shouldRetry(errorType, retryCount)) {
            val backoffDelay = retryPolicy.calculateBackoffDelay(errorType, retryCount)
            log.warn("Retrying batch ${request.id} after ${backoffDelay}ms (attempt ${retryCount + 1}/${properties.maxRetryAttempts})")
            delay(backoffDelay)
            return processSingleBatchWithRetry(request, retryCount + 1)
        }

        return resultProcessor.createFailureResult(
            request,
            exception.message ?: "Unknown error",
            errorType
        )
    }

    private fun logBatchCompletion(batchId: String, startTime: Long) {
        val duration = System.currentTimeMillis() - startTime
        log.info("Batch $batchId completed in ${duration}ms")
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

        val summaryFlow = batchSummarizer.summarizeBatch(
            inputs,
            GeminiModelType.GEMINI_2_5_FLASH_LITE
        )

        val results = mutableListOf<com.techinsights.domain.dto.gemini.SummaryResultWithId>()
        summaryFlow.collect { result ->
            results.add(result)
        }

        val batchResponse = BatchSummaryResponse(results)

        return resultProcessor.processBatchResponse(request, batchResponse, startTime)
    }
}
