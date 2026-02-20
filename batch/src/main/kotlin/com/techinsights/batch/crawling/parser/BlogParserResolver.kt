package com.techinsights.batch.crawling.parser

import org.springframework.stereotype.Component

@Component
class BlogParserResolver(
  private val parsers: List<BlogParser>
) {
  fun resolve(feedUrl: String): BlogParser =
    parsers.firstOrNull { it.supports(feedUrl) }
      ?: throw IllegalArgumentException("지원하지 않는 블로그/피드: $feedUrl")
}
