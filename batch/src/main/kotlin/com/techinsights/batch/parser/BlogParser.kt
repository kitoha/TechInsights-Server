package com.techinsights.batch.parser

import com.techinsights.domain.dto.post.PostDto

interface BlogParser {
  fun supports(feedUrl: String): Boolean
  suspend fun parseList(feedUrl: String, content: String): List<PostDto>
}