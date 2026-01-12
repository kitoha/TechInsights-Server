package com.techinsights.batch.reader

import com.techinsights.batch.builder.DynamicBatchBuilder
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.math.min

@Component
@StepScope
class BatchAwarePostReader(
    private val postRepository: PostRepository,
    private val batchBuilder: DynamicBatchBuilder,
    @Value("#{jobParameters['limit']}") private val maxCount: Long
) : ItemStreamReader<List<PostDto>> {

    private val log = LoggerFactory.getLogger(BatchAwarePostReader::class.java)

    private var offset = 0L
    private var readCount = 0L
    private var allBatches: List<DynamicBatchBuilder.Batch> = emptyList()
    private var currentBatchIndex = 0

    override fun read(): List<PostDto>? {
        if (currentBatchIndex >= allBatches.size) {
            if (readCount >= maxCount) return null

            val remaining = maxCount - readCount
            val fetchSize = min(100, remaining)

            val posts = postRepository.findOldestNotSummarized(fetchSize, offset)
                .filter { isValidForSummary(it) }

            if (posts.isEmpty()) return null

            offset += fetchSize
            allBatches = batchBuilder.buildBatches(posts)
            currentBatchIndex = 0

            log.info("Loaded ${posts.size} posts, created ${allBatches.size} batches")
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

    private fun isValidForSummary(post: PostDto): Boolean {
        if (post.content.isBlank()) {
            log.warn("Post ${post.id} has blank content, skipping")
            return false
        }

        if (post.content.length < 100) {
            log.warn("Post ${post.id} content too short (${post.content.length} chars), skipping")
            return false
        }

        if (post.title.isBlank()) {
            log.warn("Post ${post.id} has blank title, skipping")
            return false
        }

        return true
    }

    override fun open(executionContext: ExecutionContext) {
        offset = executionContext.getLong(OFFSET_KEY, 0L)
        readCount = executionContext.getLong(READ_COUNT_KEY, 0L)
        log.info("Opened reader: offset=$offset, readCount=$readCount")
    }

    override fun update(executionContext: ExecutionContext) {
        executionContext.putLong(OFFSET_KEY, offset)
        executionContext.putLong(READ_COUNT_KEY, readCount)
    }

    override fun close() {
        log.info("Closed reader: processed $readCount items")
    }

    companion object {
        private const val OFFSET_KEY = "batchAwarePostReader.offset"
        private const val READ_COUNT_KEY = "batchAwarePostReader.readCount"
    }
}
