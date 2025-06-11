package com.techinsights.parser

import com.techinsights.dto.post.PostDto

interface BlogParser {
  fun supports(feedUrl: String): Boolean
  suspend fun parseList(feedUrl: String, content: String): List<PostDto>
}