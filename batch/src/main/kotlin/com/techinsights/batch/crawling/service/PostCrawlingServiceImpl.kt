package com.techinsights.batch.crawling.service

import com.techinsights.batch.crawling.parser.BlogParserResolver
import com.techinsights.batch.crawling.util.UrlValidator
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.batch.crawling.ratelimiter.DomainRateLimiterManager
import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class PostCrawlingServiceImpl (
  private val webClient: WebClient,
  private val parserResolver: BlogParserResolver,
  private val rateLimiterManager: DomainRateLimiterManager,
  private val urlValidator: UrlValidator
): PostCrawlingService {

  override suspend fun processCrawledData(companyDto: CompanyDto): List<PostDto> {
    require(urlValidator.isSafe(companyDto.blogUrl)) { "Blocked unsafe URL: ${companyDto.blogUrl}" }

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
