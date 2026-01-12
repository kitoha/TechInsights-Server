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

@Component
@StepScope
class PostReader(
  private val postRepository: PostRepository,
  @Value("#{jobParameters['limit']}") private val maxCount: Long
) : ItemStreamReader<PostDto> {

  private val log = LoggerFactory.getLogger(PostReader::class.java)
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

      val rawPosts = postRepository
        .findOldestNotSummarized(fetchSize, lastPublishedAt, lastId)

      buffer = rawPosts.filter { isValidForSummary(it) }.toMutableList()

      rawPosts.lastOrNull()?.let { lastPost ->
        lastPublishedAt = lastPost.publishedAt
        lastId = Tsid.decode(lastPost.id)
      }

      if (buffer.isEmpty()) return null
    }

    readCount++
    return buffer.removeFirst()
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
    // no resources to close
  }

  companion object {
    private const val DEFAULT_PAGE_SIZE = 100
    private const val PUBLISHED_AT_KEY = "postReader.cursor.publishedAt"
    private const val ID_KEY = "postReader.cursor.id"
    private const val READ_COUNT_KEY = "postReader.readCount"
  }
}