package com.techinsights.batch.reader

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

/**
 * Reader that reads summarized posts in batches for efficient embedding processing.
 *
 * This reader returns a List<PostDto> instead of a single PostDto, allowing the
 * processor to generate embeddings for multiple posts in a single API call.
 *
 * Batch size is configurable via job parameters, with a default of 50 to stay
 * well under the Gemini API limit of 100 requests per minute.
 */
@Component
@StepScope
class BatchSummarizedPostReader(
    private val postRepository: PostRepository,
    @Value("#{jobParameters['limit'] ?: 1000L}") private val maxCount: Long,
    @Value("#{jobParameters['embeddingBatchSize'] ?: 50}") private val batchSize: Int
) : ItemStreamReader<List<PostDto>> {

    private val log = LoggerFactory.getLogger(BatchSummarizedPostReader::class.java)

    private var lastPublishedAt: LocalDateTime? = null
    private var lastId: Long? = null
    private var readCount = 0L

    override fun read(): List<PostDto>? {
        if (readCount >= maxCount) {
            log.info("Reached max count limit: $maxCount")
            return null
        }

        val remaining = maxCount - readCount
        val fetchSize = min(batchSize.toLong(), remaining)

        if (fetchSize <= 0) return null

        val posts = postRepository
            .findOldestSummarizedAndNotEmbedded(fetchSize, lastPublishedAt, lastId)

        if (posts.isEmpty()) {
            log.info("No more posts to process")
            return null
        }

        posts.lastOrNull()?.let { lastPost ->
            lastPublishedAt = lastPost.publishedAt
            lastId = Tsid.decode(lastPost.id)
        }

        readCount += posts.size

        log.info(
            "Read batch of ${posts.size} posts " +
            "(total read: $readCount/$maxCount, cursor: publishedAt=$lastPublishedAt, id=$lastId)"
        )

        return posts
    }

    override fun open(executionContext: ExecutionContext) {
        if (executionContext.containsKey(PUBLISHED_AT_KEY) && executionContext.containsKey(ID_KEY)) {
            val savedPublishedAt = executionContext.getString(PUBLISHED_AT_KEY)
            val savedId = executionContext.getLong(ID_KEY)

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
        log.info("Closed reader: processed $readCount posts (final cursor: publishedAt=$lastPublishedAt, id=$lastId)")
    }

    companion object {
        private const val PUBLISHED_AT_KEY = "batchSummarizedPostReader.cursor.publishedAt"
        private const val ID_KEY = "batchSummarizedPostReader.cursor.id"
        private const val READ_COUNT_KEY = "batchSummarizedPostReader.readCount"
    }
}
