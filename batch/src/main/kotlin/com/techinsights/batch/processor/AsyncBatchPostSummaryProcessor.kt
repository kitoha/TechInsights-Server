package com.techinsights.batch.processor

import com.techinsights.batch.builder.DynamicBatchBuilder
import com.techinsights.batch.dto.BatchRequest
import com.techinsights.batch.service.AsyncBatchSummarizationService
import com.techinsights.domain.dto.post.PostDto
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import java.util.*

@Component
class AsyncBatchPostSummaryProcessor(
    private val batchService: AsyncBatchSummarizationService,
    private val batchBuilder: DynamicBatchBuilder
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

        val batchRequests = batches.map { batch ->
            BatchRequest(
                id = UUID.randomUUID().toString(),
                posts = batch.items,
                estimatedTokens = batch.estimatedTokens,
                priority = 0
            )
        }

        log.info("Created ${batchRequests.size} API batches from ${items.size} posts")

        val batchResults = batchService.processBatchesAsync(batchRequests)

        val allSuccesses = batchResults.flatMap { it.successes }
        val allFailures = batchResults.flatMap { it.failures }

        log.info("Batch processing complete: ${allSuccesses.size} successes, ${allFailures.size} failures")

        if (allFailures.isNotEmpty()) {
            val retryableCount = allFailures.count { it.retryable }
            val nonRetryableCount = allFailures.size - retryableCount

            log.warn("${allFailures.size} posts failed: $retryableCount retryable, $nonRetryableCount non-retryable")

            allFailures.take(5).forEach { failure ->
                log.debug("Failed post ${failure.post.id}: ${failure.reason} (retryable: ${failure.retryable})")
            }
        }

        val successIds = allSuccesses.map { it.id }.toSet()
        val failedPosts = items.filter { it.id !in successIds }

        return allSuccesses + failedPosts
    }
}
