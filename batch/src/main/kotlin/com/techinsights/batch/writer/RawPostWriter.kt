package com.techinsights.batch.writer

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.service.company.CompanyViewCountUpdater
import com.techinsights.domain.service.gemini.GeminiArticleSummarizer
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.WriteFailedException
import org.springframework.stereotype.Component

@Component
class RawPostWriter(
  private val postRepository: PostRepository,
  private val companyViewCountUpdater: CompanyViewCountUpdater,
  private val geminiArticleSummarizer: GeminiArticleSummarizer
) : ItemWriter<List<PostDto>>{

  override fun write(chunk: Chunk<out List<PostDto>>) {
    if (chunk.isEmpty) {
      return
    }

    try {
      val allPosts = chunk.items.flatten()

      val originalUrls = allPosts.map { it.url }
      val existUrls = postRepository.findAllByUrlIn(originalUrls).map { it.url }.toSet()

      val filteredPosts = allPosts.filter { it.url !in existUrls }

      if (filteredPosts.isNotEmpty()) {
        filteredPosts.map { post ->
          val summaryResult = geminiArticleSummarizer.summarize(post.content)
          post.content = summaryResult.summary
          post.category = summaryResult.categories.map { Category.valueOf(it) }.toSet()
          post.toEntity()
        }

        val savedPosts = postRepository.saveAll(filteredPosts)

        val companyPostCountMap = savedPosts.groupingBy { it.company.id }.eachCount()

        companyPostCountMap.forEach { (companyId, postCount) ->
          companyViewCountUpdater.incrementPostCount(companyId, postCount)
        }

        log.info("Saved ${savedPosts.size} new posts (filtered from ${allPosts.size})")
      } else {
        log.info("No new posts to save (all duplicates)")
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