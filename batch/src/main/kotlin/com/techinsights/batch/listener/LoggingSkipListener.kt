package com.techinsights.batch.listener

import com.techinsights.domain.dto.post.PostDto
import org.slf4j.LoggerFactory
import org.springframework.batch.core.SkipListener
import org.springframework.stereotype.Component

@Component
class LoggingSkipListener : SkipListener<PostDto, PostDto> {

  private val log = LoggerFactory.getLogger(LoggingJobExecutionListener::class.java)

  override fun onSkipInRead(t: Throwable) {
    log.warn("Reader processing failed, skipping item due to: ${t.message}")
  }

  override fun onSkipInWrite(item: PostDto, t: Throwable) {
    log.warn("Writer processing failed for post id=${item.id}, skipping item due to: ${t.message}")
  }

  override fun onSkipInProcess(item: PostDto, t: Throwable) {
    log.warn("Processor processing failed for post id=${item.id}, skipping item due to: ${t.message}")
  }
}