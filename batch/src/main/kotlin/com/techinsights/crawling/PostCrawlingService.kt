package com.techinsights.crawling

interface PostCrawlingService {
  suspend fun processCrawledData(feedUrl: String): List<RawPost>
  fun saveProcessedData(processedData: String): Boolean
}