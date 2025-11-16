package com.techinsights.batch.writer

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.service.company.CompanyViewCountUpdater
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class PostWriter(
  private val postRepository: PostRepository,
  private val companyViewCountUpdater: CompanyViewCountUpdater
) : ItemWriter<PostDto> {

  private val log = LoggerFactory.getLogger(PostWriter::class.java)

  override fun write(chunk: Chunk<out PostDto>) {
    val postsToWrite = chunk.items
    if (postsToWrite.isNotEmpty()) {
      val summarizedItem = postsToWrite.filter { it.isSummary }
      val savedPosts = postRepository.saveAll(summarizedItem)

      val companyPostCountMap = savedPosts.groupingBy { it.company.id }.eachCount()

      companyPostCountMap.forEach { (companyId, postCount) ->
        companyViewCountUpdater.incrementPostCount(companyId, postCount)
      }

      log.info("Successfully saved ${savedPosts.size} summarized posts and updated postCount")
    }
  }
}
