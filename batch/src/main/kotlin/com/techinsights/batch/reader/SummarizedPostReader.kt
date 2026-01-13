package com.techinsights.batch.reader

import com.techinsights.batch.reader.base.AbstractCursorBasedReader
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.math.min

@Component
@StepScope
class SummarizedPostReader(
    private val postRepository: PostRepository,
    @Value("#{jobParameters['limit'] ?: 100L}") private val maxCount: Long
) : AbstractCursorBasedReader<PostDto>() {

    private val pageSize = DEFAULT_PAGE_SIZE
    private var buffer: MutableList<PostDto> = mutableListOf()

    override fun getContextKeyPrefix(): String = "summarizedPostReader"
    override fun getMaxCount(): Long = maxCount

    override fun read(): PostDto? {
        if (hasReachedLimit()) {
            return null
        }

        if (buffer.isEmpty()) {
            val remaining = calculateRemaining()
            val fetchSize = min(pageSize.toLong(), remaining)

            if (fetchSize <= 0) return null

            buffer = postRepository
                .findOldestSummarizedAndNotEmbedded(fetchSize, lastPublishedAt, lastId)
                .toMutableList()

            buffer.lastOrNull()?.let { lastPost ->
                updateCursor(lastPost.publishedAt, lastPost.id)
            }

            if (buffer.isEmpty()) return null
        }

        readCount++
        return buffer.removeFirst()
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 100
    }
}
