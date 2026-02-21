package com.techinsights.batch.summary.reader

import com.techinsights.batch.summary.builder.DynamicBatchBuilder
import com.techinsights.batch.common.reader.base.AbstractCursorBasedReader
import com.techinsights.batch.summary.reader.validator.PostValidator
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.math.min

@Component
@StepScope
class BatchAwarePostReader(
    private val postRepository: PostRepository,
    private val batchBuilder: DynamicBatchBuilder,
    @Value("#{jobParameters['limit']}") private val maxCount: Long
) : AbstractCursorBasedReader<List<PostDto>>() {

    private var allBatches: List<DynamicBatchBuilder.Batch> = emptyList()
    private var currentBatchIndex = 0

    override fun getContextKeyPrefix(): String = "batchAwarePostReader"
    override fun getMaxCount(): Long = maxCount

    override fun read(): List<PostDto>? {
        if (currentBatchIndex >= allBatches.size) {
            if (hasReachedLimit()) return null

            val remaining = calculateRemaining()
            val fetchSize = min(100, remaining)

            val posts = postRepository.findOldestNotSummarized(fetchSize, lastPublishedAt, lastId)
                .filter { PostValidator.isValidForSummary(it) }

            if (posts.isEmpty()) return null

            posts.lastOrNull()?.let { lastPost ->
                updateCursor(lastPost.publishedAt, lastPost.id)
            }

            allBatches = batchBuilder.buildBatches(posts)
            currentBatchIndex = 0

            log.info("Loaded ${posts.size} posts, created ${allBatches.size} batches (cursor: publishedAt=$lastPublishedAt, id=$lastId)")
        }

        if (currentBatchIndex < allBatches.size) {
            val batch = allBatches[currentBatchIndex]
            currentBatchIndex++
            readCount += batch.items.size

            log.debug("Reading batch $currentBatchIndex/${allBatches.size} with ${batch.items.size} items (${batch.estimatedTokens} tokens)")

            return batch.items
        }

        return null
    }
}
