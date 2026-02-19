package com.techinsights.batch.crawling

import com.techinsights.batch.parser.BlogParserResolver
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.ratelimiter.DomainRateLimiterManager
import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class PostCrawlingServiceImpl (
  private val webClient: WebClient,
  private val parserResolver: BlogParserResolver,
  private val rateLimiterManager: DomainRateLimiterManager
): PostCrawlingService {

  override suspend fun processCrawledData(companyDto: CompanyDto): List<PostDto> {
    val rateLimiter = rateLimiterManager.getRateLimiter(companyDto.blogUrl)

    val rssContent = rateLimiter.executeSuspendFunction {
      fetchRssContent(companyDto.blogUrl)
    }
    val parser = parserResolver.resolve(companyDto.blogUrl)
    return parser.parseList(companyDto, rssContent)
  }

  private suspend fun fetchRssContent(feedUrl: String): String {
        return webClient.get()
          .uri(feedUrl)
          .retrieve()
          .awaitBody()
  }
}
