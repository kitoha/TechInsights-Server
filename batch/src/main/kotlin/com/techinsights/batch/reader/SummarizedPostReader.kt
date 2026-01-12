package com.techinsights.batch.reader

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.utils.Tsid
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import kotlin.math.min

@Component
@StepScope
class SummarizedPostReader(
    private val postRepository: PostRepository,
    @Value("#{jobParameters['limit'] ?: 100L}") private val maxCount: Long
) : ItemStreamReader<PostDto> {

    private val pageSize = DEFAULT_PAGE_SIZE
    private var lastPublishedAt: LocalDateTime? = null
    private var lastId: Long? = null
    private var readCount = 0L
    private var buffer: MutableList<PostDto> = mutableListOf()

    override fun read(): PostDto? {
        if (readCount >= maxCount) return null

        if (buffer.isEmpty()) {
            val remaining = maxCount - readCount
            val fetchSize = min(pageSize.toLong(), remaining)

            if (fetchSize <= 0) return null

            buffer = postRepository
                .findOldestSummarizedAndNotEmbedded(fetchSize, lastPublishedAt, lastId)
                .toMutableList()

            buffer.lastOrNull()?.let { lastPost ->
                lastPublishedAt = lastPost.publishedAt
                lastId = Tsid.decode(lastPost.id)
            }

            if (buffer.isEmpty()) return null
        }

        readCount++
        return buffer.removeFirst()
    }

    override fun open(executionContext: ExecutionContext) {
        if (executionContext.containsKey(PUBLISHED_AT_KEY) && executionContext.containsKey(ID_KEY)) {
            val savedPublishedAt = executionContext.getString(PUBLISHED_AT_KEY)
            val savedId = executionContext.getLong(ID_KEY)
            
            lastPublishedAt = LocalDateTime.parse(savedPublishedAt)
            lastId = savedId
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
        executionContext.putLong(READ_COUNT_KEY, this.readCount)
    }

    override fun close() {
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 100
        private const val PUBLISHED_AT_KEY = "summarizedPostReader.cursor.publishedAt"
        private const val ID_KEY = "summarizedPostReader.cursor.id"
        private const val READ_COUNT_KEY = "summarizedPostReader.readCount"
    }
}