package com.techinsights.processor

import com.techinsights.crawling.PostCrawlingService
import com.techinsights.dto.post.PostDto
import com.techinsights.entity.company.Company
import kotlinx.coroutines.runBlocking
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class RawPostProcessor(
  private val postCrawlingService: PostCrawlingService
) : ItemProcessor<Company, List<PostDto>> {
  override fun process(company: Company): List<PostDto> = runBlocking {
    try {
      postCrawlingService.processCrawledData(company.blogUrl)
    } catch (e: Exception) {
      log.error("Feed 파싱 실패 [${company.name}] - ${e.message}")
      emptyList()
    }
  }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(RawPostProcessor::class.java)
  }
}
