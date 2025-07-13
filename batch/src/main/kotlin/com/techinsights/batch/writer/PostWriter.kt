package com.techinsights.batch.writer

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class PostWriter(
  private val postRepository: PostRepository
) : ItemWriter<List<PostDto>> {

  override fun write(chunk: Chunk<out List<PostDto>>) {
    val postsToWrite = chunk.items.flatten()
    if (postsToWrite.isNotEmpty()) {
      postRepository.saveAll(postsToWrite)
    }
  }
}