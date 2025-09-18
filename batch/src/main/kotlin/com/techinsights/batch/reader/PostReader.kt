package com.techinsights.batch.reader

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.math.min

@Component
@StepScope
class PostReader(
  private val postRepository: PostRepository,
  @Value("#{jobParameters['limit']}") private val maxCount: Long
) : ItemStreamReader<PostDto> {

  private val pageSize = DEFAULT_PAGE_SIZE
  private var offset = 0L
  private var readCount = 0L
  private var buffer: MutableList<PostDto> = mutableListOf()

  override fun read(): PostDto? {
    if (readCount >= maxCount) return null

    if (buffer.isEmpty()) {
      val remaining = maxCount - readCount
      val fetchSize = min(pageSize.toLong(), remaining)

      if (fetchSize <= 0) return null

      buffer = postRepository
        .findOldestNotSummarized(fetchSize, offset)
        .toMutableList()

      offset += fetchSize
      if (buffer.isEmpty()) return null
    }

    readCount++
    return buffer.removeFirst()
  }

  override fun open(executionContext: ExecutionContext) {
    if (executionContext.containsKey(OFFSET_KEY)) {
      this.offset = executionContext.getLong(OFFSET_KEY)
      this.readCount = executionContext.getLong(READ_COUNT_KEY)
    } else {
      this.offset = 0L
      this.readCount = 0L
    }
  }

  override fun update(executionContext: ExecutionContext) {
    executionContext.putLong(OFFSET_KEY, this.offset)
    executionContext.putLong(READ_COUNT_KEY, this.readCount)
  }

  override fun close() {
  }

  companion object {
    private const val DEFAULT_PAGE_SIZE = 100
    private const val OFFSET_KEY = "postReader.offset"
    private const val READ_COUNT_KEY = "postReader.readCount"
  }
}