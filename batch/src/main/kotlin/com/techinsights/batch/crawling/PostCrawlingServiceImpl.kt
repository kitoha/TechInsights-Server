package com.techinsights.batch.crawling

import com.techinsights.batch.parser.BlogParserResolver
import com.techinsights.domain.dto.post.PostDto
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class PostCrawlingServiceImpl (
  private val webClient: WebClient,
  private val parserResolver: BlogParserResolver
): PostCrawlingService {

  override suspend fun processCrawledData(feedUrl: String): List<PostDto> {
    val rssContent = fetchRssContent(feedUrl)
    val parser = parserResolver.resolve(feedUrl)
    return parser.parseList(feedUrl, rssContent)
  }

  private suspend fun fetchRssContent(feedUrl: String): String {
        return webClient.get()
          .uri(feedUrl)
          .retrieve()
          .awaitBody()
  }
}