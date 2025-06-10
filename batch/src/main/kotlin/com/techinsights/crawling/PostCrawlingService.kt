package com.techinsights.crawling

import com.techinsights.dto.PostDto

interface PostCrawlingService {
  suspend fun processCrawledData(feedUrl: String): List<PostDto>
  fun saveProcessedData(processedData: String): Boolean
}