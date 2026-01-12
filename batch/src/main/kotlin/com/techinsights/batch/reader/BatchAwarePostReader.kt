package com.techinsights.batch.reader

import com.techinsights.batch.builder.DynamicBatchBuilder
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.utils.Tsid
import org.slf4j.LoggerFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import kotlin.math.min

@Component
@StepScope
class BatchAwarePostReader(
    private val postRepository: PostRepository,
    private val batchBuilder: DynamicBatchBuilder,
    @Value("#{jobParameters['limit']}") private val maxCount: Long
) : ItemStreamReader<List<PostDto>> {

    private val log = LoggerFactory.getLogger(BatchAwarePostReader::class.java)

    private var lastPublishedAt: LocalDateTime? = null
    private var lastId: Long? = null
    private var readCount = 0L
    private var allBatches: List<DynamicBatchBuilder.Batch> = emptyList()
    private var currentBatchIndex = 0

    override fun read(): List<PostDto>? {
        if (currentBatchIndex >= allBatches.size) {
            if (readCount >= maxCount) return null

            val remaining = maxCount - readCount
            val fetchSize = min(100, remaining)

            val posts = postRepository.findOldestNotSummarized(fetchSize, lastPublishedAt, lastId)
                .filter { isValidForSummary(it) }

            if (posts.isEmpty()) return null

            posts.lastOrNull()?.let { lastPost ->
                lastPublishedAt = lastPost.publishedAt
                lastId = Tsid.decode(lastPost.id)
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
        val savedPublishedAt = executionContext.getString(PUBLISHED_AT_KEY)
        val savedId = executionContext.getLong(ID_KEY, -1L)

        if (savedPublishedAt != null && savedId != -1L) {
            lastPublishedAt = LocalDateTime.parse(savedPublishedAt)
            lastId = savedId
            log.info("Resumed from cursor: publishedAt=$lastPublishedAt, id=$lastId")
        } else {
            log.info("Starting from beginning (no cursor found)")
        }

        readCount = executionContext.getLong(READ_COUNT_KEY, 0L)
    }

    override fun update(executionContext: ExecutionContext) {
        lastPublishedAt?.let {
            executionContext.putString(PUBLISHED_AT_KEY, it.toString())
        }
        lastId?.let {
            executionContext.putLong(ID_KEY, it)
        }
        executionContext.putLong(READ_COUNT_KEY, readCount)
    }

    override fun close() {
        log.info("Closed reader: processed $readCount items (final cursor: publishedAt=$lastPublishedAt, id=$lastId)")
    }

    companion object {
        private const val PUBLISHED_AT_KEY = "batchAwarePostReader.cursor.publishedAt"
        private const val ID_KEY = "batchAwarePostReader.cursor.id"
        private const val READ_COUNT_KEY = "batchAwarePostReader.readCount"
    }
}
