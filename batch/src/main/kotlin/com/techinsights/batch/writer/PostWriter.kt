package com.techinsights.batch.writer

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.entity.post.Post
import com.techinsights.domain.repository.post.PostJpaRepository
import com.techinsights.domain.repository.post.PostRepository
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class PostWriter(
  private val postRepository: PostRepository
) : ItemWriter<PostDto> {

  override fun write(chunk: Chunk<out PostDto>) {
    if (chunk.isEmpty) return

    postRepository.saveAll(chunk.items)
  }
}