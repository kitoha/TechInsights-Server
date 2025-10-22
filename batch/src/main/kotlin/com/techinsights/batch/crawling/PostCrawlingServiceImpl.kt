package com.techinsights.batch.crawling

import com.techinsights.batch.parser.BlogParserResolver
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import kotlinx.coroutines.delay
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class PostCrawlingServiceImpl (
  private val webClient: WebClient,
  private val parserResolver: BlogParserResolver
): PostCrawlingService {

  override suspend fun processCrawledData(companyDto: CompanyDto): List<PostDto> {
    val rssContent = fetchRssContent(companyDto.blogUrl)
    val parser = parserResolver.resolve(companyDto.blogUrl)
    val posts = parser.parseList(companyDto, rssContent)
    delay(1000)
    return posts
  }

  private suspend fun fetchRssContent(feedUrl: String): String {
        return webClient.get()
          .uri(feedUrl)
          .retrieve()
          .awaitBody()
  }
}