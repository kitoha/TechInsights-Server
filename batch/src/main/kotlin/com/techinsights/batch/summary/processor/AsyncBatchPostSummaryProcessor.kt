package com.techinsights.batch.summary.processor

import com.techinsights.batch.summary.builder.DynamicBatchBuilder
import com.techinsights.batch.summary.dto.BatchRequest
import com.techinsights.batch.summary.service.AsyncBatchSummarizationService
import com.techinsights.domain.dto.post.PostDto
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import java.util.*

@Component
class AsyncBatchPostSummaryProcessor(
    private val batchService: AsyncBatchSummarizationService,
    private val batchBuilder: DynamicBatchBuilder,
    private val failureMapper: FailurePostMapper
) : ItemProcessor<List<PostDto>, List<PostDto>> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun process(items: List<PostDto>): List<PostDto>? {
        if (items.isEmpty()) return null

        log.info("Processing batch of ${items.size} posts")

        return runBlocking {
            processAsync(items)
        }
    }

    private suspend fun processAsync(items: List<PostDto>): List<PostDto> {
        val batches = batchBuilder.buildBatches(items)
        val batchRequests = createBatchRequests(batches)

        log.info("Created ${batchRequests.size} API batches from ${items.size} posts")

        val batchResults = batchService.processBatchesAsync(batchRequests)

        val allSuccesses = batchResults.flatMap { it.successes }
        val allFailures = batchResults.flatMap { it.failures }

        logProcessingResults(allSuccesses.size, allFailures)

        val failedPosts = failureMapper.mapFailuresToPosts(items, allSuccesses, allFailures)

        return allSuccesses + failedPosts
    }

    private fun createBatchRequests(batches: List<DynamicBatchBuilder.Batch>): List<BatchRequest> {
        return batches.map { batch ->
            BatchRequest(
                id = UUID.randomUUID().toString(),
                posts = batch.items,
                estimatedTokens = batch.estimatedTokens,
                priority = 0
            )
        }
    }

    private fun logProcessingResults(successCount: Int, failures: List<com.techinsights.batch.summary.dto.BatchFailure>) {
        log.info("Batch processing complete: $successCount successes, ${failures.size} failures")

        if (failures.isNotEmpty()) {
            val retryableCount = failures.count { it.retryable }
            val nonRetryableCount = failures.size - retryableCount

            log.warn("${failures.size} posts failed: $retryableCount retryable, $nonRetryableCount non-retryable")

            failures.take(5).forEach { failure ->
                log.debug("Failed post ${failure.post.id}: ${failure.reason} (retryable: ${failure.retryable})")
            }
        }
    }
}
