package com.techinsights.batch.crawling.writer

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
      val newPosts = chunk.items.flatten()
        .distinctBy { it.url }
        .filter { it.content.isNotBlank() }

      if (newPosts.isNotEmpty()) {
        val insertedCount = postRepository.insertAllIgnoringDuplicates(newPosts)

        log.info("[Writer] Inserted {} new posts out of {} candidates", insertedCount, newPosts.size)
      } else {
        log.info("[Writer] No new posts to save (all candidates were duplicates or empty)")
      }

    } catch (e: Exception) {
      log.error("Unexpected error while saving posts from ${chunk.size()} companies: ${e.message}", e)
      throw WriteFailedException("Unexpected error during post save operation", e)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(RawPostWriter::class.java)
  }

}