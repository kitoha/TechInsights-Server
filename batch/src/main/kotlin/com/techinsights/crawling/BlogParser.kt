package com.techinsights.crawling

import com.techinsights.dto.Post

interface BlogParser {
  fun supports(feedUrl: String): Boolean
  suspend fun parseList(feedUrl: String, content: String): List<RawPost>
}