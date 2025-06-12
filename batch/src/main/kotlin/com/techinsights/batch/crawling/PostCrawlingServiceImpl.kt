package com.techinsights.batch.crawling

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.batch.parser.RssParser
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class PostCrawlingServiceImpl (
  private val webClient: WebClient,
  private val rssParser: RssParser
): PostCrawlingService {

    override suspend fun processCrawledData(feedUrl: String): List<PostDto> {
        val rssContent = fetchRssContent(feedUrl)
        val rawPosts = rssParser.parseList(feedUrl, rssContent)

        return rawPosts
    }

    override fun saveProcessedData(processedData: String): Boolean {
        // Simulate saving the processed data
        println("Saving processed data: $processedData")
        return true
    }

  private suspend fun fetchRssContent(feedUrl: String): String {
        return webClient.get()
          .uri(feedUrl)
          .retrieve()
          .awaitBody()
  }
}