package com.techinsights.batch.reader

import com.techinsights.batch.reader.base.AbstractCursorBasedReader
import com.techinsights.batch.reader.validator.PostValidator
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.math.min

@Component
@StepScope
class PostReader(
    private val postRepository: PostRepository,
    @Value("#{jobParameters['limit']}") private val maxCount: Long
) : AbstractCursorBasedReader<PostDto>() {

    private val pageSize = DEFAULT_PAGE_SIZE
    private var buffer: MutableList<PostDto> = mutableListOf()

    override fun getContextKeyPrefix(): String = "postReader"
    override fun getMaxCount(): Long = maxCount

    override fun read(): PostDto? {
        if (hasReachedLimit()) {
            return null
        }

        if (buffer.isEmpty()) {
            val remaining = calculateRemaining()
            val fetchSize = min(pageSize.toLong(), remaining)

            if (fetchSize <= 0) return null

            val rawPosts = postRepository.findOldestNotSummarized(fetchSize, lastPublishedAt, lastId)

            buffer = rawPosts.filter { PostValidator.isValidForSummary(it) }.toMutableList()

            rawPosts.lastOrNull()?.let { lastPost ->
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
