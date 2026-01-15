package com.techinsights.batch.builder

import com.techinsights.batch.config.BatchBuildConfig
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.utils.TokenEstimator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DynamicBatchBuilder(
    private val config: BatchBuildConfig,
    private val limitChecker: BatchLimitChecker,
    private val postTruncator: PostTruncator
) {

    private val log = LoggerFactory.getLogger(javaClass)

    data class Batch(
        val items: List<PostDto>,
        val estimatedTokens: Int
    )

    fun buildBatches(posts: List<PostDto>): List<Batch> {
        val batches = mutableListOf<Batch>()
        var currentBatch = mutableListOf<PostDto>()
        var currentTokens = config.basePromptTokens

        for (post in posts) {
            val postTokens = TokenEstimator.estimateTotalTokens(post.content)

            if (limitChecker.exceedsMaxTokens(postTokens)) {
                handleOversizedPost(post, batches, currentBatch, currentTokens)
                currentBatch = mutableListOf()
                currentTokens = config.basePromptTokens
                continue
            }

            if (shouldStartNewBatch(currentBatch.size, currentTokens, postTokens)) {
                finalizeBatch(batches, currentBatch, currentTokens)
                currentBatch = mutableListOf()
                currentTokens = config.basePromptTokens
            }

            currentBatch.add(post)
            currentTokens += postTokens
        }

        finalizeBatch(batches, currentBatch, currentTokens)
        logBatchSummary(batches, posts.size)

        return batches
    }

    private fun shouldStartNewBatch(
        currentSize: Int,
        currentTokens: Int,
        additionalTokens: Int
    ): Boolean {
        if (currentSize == 0) return false

        val exceedsInput = limitChecker.exceedsInputLimit(currentTokens, additionalTokens)
        val exceedsOutput = limitChecker.exceedsOutputLimit(currentSize + 1)
        val exceedsSize = limitChecker.exceedsBatchSize(currentSize)

        if (exceedsOutput) {
            log.debug(
                "Starting new batch: would exceed output limit " +
                "(${limitChecker.estimateOutputTokens(currentSize + 1)} > ${limitChecker.maxOutputTokensAllowed})"
            )
        }

        return exceedsInput || exceedsOutput || exceedsSize
    }

    private fun handleOversizedPost(
        post: PostDto,
        batches: MutableList<Batch>,
        currentBatch: MutableList<PostDto>,
        currentTokens: Int
    ) {
        log.warn("Post ${post.id} too large, truncating")

        if (currentBatch.isNotEmpty()) {
            batches.add(Batch(currentBatch.toList(), currentTokens))
        }

        val maxTokens = postTruncator.calculateMaxTokensForTruncation()
        val truncatedPost = postTruncator.truncate(post, maxTokens)
        val truncatedTokens = TokenEstimator.estimateTotalTokens(truncatedPost.content)

        batches.add(Batch(listOf(truncatedPost), truncatedTokens))
    }

    private fun finalizeBatch(
        batches: MutableList<Batch>,
        currentBatch: List<PostDto>,
        currentTokens: Int
    ) {
        if (currentBatch.isNotEmpty()) {
            batches.add(Batch(currentBatch.toList(), currentTokens))
        }
    }

    private fun logBatchSummary(batches: List<Batch>, totalPosts: Int) {
        val avgBatchSize = if (batches.isNotEmpty()) totalPosts / batches.size else 0
        val maxOutput = limitChecker.maxOutputTokensAllowed

        log.info(
            "Built ${batches.size} batches from $totalPosts posts. " +
            "Avg batch size: $avgBatchSize, Max output tokens: $maxOutput"
        )
    }
}
