package com.techinsights.batch.crawling.processor

import com.techinsights.batch.crawling.service.PostCrawlingService
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
    val startTime = System.currentTimeMillis()
    log.info(">>>> [Crawling Start] Company: {} | URL: {}", company.name, company.blogUrl)

    try {
      val result = postCrawlingService.processCrawledData(company)

      val duration = System.currentTimeMillis() - startTime
      log.info("<<<< [Crawling End]   Company: {} | Items: {} | Time: {}ms", company.name, result.size, duration)

      result
    } catch (e: Exception) {
      val duration = System.currentTimeMillis() - startTime
      log.error("!!!! [Crawling Fail]  Company: {} | Time: {}ms | Error: {}", company.name, duration, e.message, e)
      throw e
    }
  }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(RawPostProcessor::class.java)
  }
}
