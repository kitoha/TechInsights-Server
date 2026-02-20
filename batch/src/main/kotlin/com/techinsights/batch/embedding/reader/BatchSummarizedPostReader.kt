package com.techinsights.batch.embedding.reader

import com.techinsights.batch.common.reader.base.AbstractCursorBasedReader
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.math.min


@Component
@StepScope
class BatchSummarizedPostReader(
    private val postRepository: PostRepository,
    @Value("#{jobParameters['limit'] ?: 1000L}") private val maxCount: Long,
    @Value("#{jobParameters['embeddingBatchSize'] ?: 50}") private val batchSize: Int
) : AbstractCursorBasedReader<List<PostDto>>() {

    override fun getContextKeyPrefix(): String = "batchSummarizedPostReader"
    override fun getMaxCount(): Long = maxCount

    override fun read(): List<PostDto>? {
        if (hasReachedLimit()) {
            log.info("Reached max count limit: $maxCount")
            return null
        }

        val remaining = calculateRemaining()
        val fetchSize = min(batchSize.toLong(), remaining)

        if (fetchSize <= 0) return null

        val posts = postRepository.findOldestSummarizedAndNotEmbedded(fetchSize, lastPublishedAt, lastId)

        if (posts.isEmpty()) {
            log.info("No more posts to process")
            return null
        }

        posts.lastOrNull()?.let { lastPost ->
            updateCursor(lastPost.publishedAt, lastPost.id)
        }

        readCount += posts.size

        log.info(
            "Read batch of ${posts.size} posts " +
            "(total read: $readCount/$maxCount, cursor: publishedAt=$lastPublishedAt, id=$lastId)"
        )

        return posts
    }
}
