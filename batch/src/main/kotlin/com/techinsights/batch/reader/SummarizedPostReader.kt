package com.techinsights.batch.reader

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.math.min

@Component
@StepScope
class SummarizedPostReader(
    private val postRepository: PostRepository,
    @Value("#{jobParameters['limit'] ?: 1000L}") private val maxCount: Long
) : ItemReader<PostDto> {

    private val pageSize = DEFAULT_PAGE_SIZE
    private var offset = 0L
    private var readCount = 0L
    private var buffer: MutableList<PostDto> = mutableListOf()

    override fun read(): PostDto? {
        if (readCount >= maxCount) return null

        if (buffer.isEmpty()) {
            val remaining = maxCount - readCount
            val fetchSize = min(pageSize.toLong(), remaining)

            buffer = postRepository
                .findOldestNotSummarized(fetchSize, offset)
                .toMutableList()

            offset += fetchSize
            if (buffer.isEmpty()) return null
        }

        readCount++
        return buffer.removeFirst()
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 100
    }
}
