package com.techinsights.batch.processor

import com.techinsights.batch.crawling.PostCrawlingService
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import kotlinx.coroutines.runBlocking
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class RawPostProcessor(
  private val postCrawlingService: PostCrawlingService
) : ItemProcessor<CompanyDto, List<PostDto>> {
  override fun process(company: CompanyDto): List<PostDto> = runBlocking {
    try {
      postCrawlingService.processCrawledData(company)
    } catch (e: Exception) {
      log.error("Feed 파싱 실패 [${company.name}] - ${e.message}")
      throw e
    }
  }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(RawPostProcessor::class.java)
  }
}
