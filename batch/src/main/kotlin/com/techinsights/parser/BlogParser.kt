package com.techinsights.parser

import com.techinsights.dto.PostDto

interface BlogParser {
  fun supports(feedUrl: String): Boolean
  suspend fun parseList(feedUrl: String, content: String): List<PostDto>
}