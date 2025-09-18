package com.techinsights.batch.writer

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class PostWriter(
  private val postRepository: PostRepository
) : ItemWriter<PostDto> {

  override fun write(chunk: Chunk<out PostDto>) {
    val postsToWrite = chunk.items
    if (postsToWrite.isNotEmpty()) {
      val summarizedItem = postsToWrite.filter { it.isSummary }
      postRepository.saveAll(summarizedItem)
    }
  }
}
