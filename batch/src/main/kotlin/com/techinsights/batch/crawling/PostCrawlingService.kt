package com.techinsights.batch.crawling

import com.techinsights.domain.dto.post.PostDto

interface PostCrawlingService {
  suspend fun processCrawledData(feedUrl: String): List<PostDto>
  fun saveProcessedData(processedData: String): Boolean
}