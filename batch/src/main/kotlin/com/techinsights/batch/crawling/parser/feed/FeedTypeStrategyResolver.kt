package com.techinsights.batch.crawling.parser.feed

import org.jsoup.nodes.Document

class FeedTypeStrategyResolver {

  private val strategies = listOf(
    AtomFeedStrategy(),
    RssFeedStrategy()
  )

  fun resolve(document: Document): FeedTypeStrategy =
    strategies.firstOrNull { it.supports(document) }
      ?: throw IllegalArgumentException("Unsupported feed type")
}
