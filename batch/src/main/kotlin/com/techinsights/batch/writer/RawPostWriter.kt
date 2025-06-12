package com.techinsights.batch.writer

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.WriteFailedException
import org.springframework.stereotype.Component

@Component
class RawPostWriter(
  private val postRepository: PostRepository
) : ItemWriter<List<PostDto>>{

  override fun write(chunk: Chunk<out List<PostDto>>) {
    if (chunk.isEmpty) {
      return
    }

    try {
      val allPosts = chunk.items.flatten()

      val savedPosts = postRepository.saveAll(allPosts)
      log.info("Successfully saved ${savedPosts.size} posts from ${chunk.size()} companies")

    } catch (e: Exception) {
      log.error("Unexpected error while saving posts from ${chunk.size()} companies: ${e.message}", e)
      throw WriteFailedException("Unexpected error during post save operation", e)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(RawPostWriter::class.java)
  }

}