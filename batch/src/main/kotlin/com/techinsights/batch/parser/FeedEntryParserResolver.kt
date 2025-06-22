package com.techinsights.batch.parser

import org.jsoup.nodes.Document


object FeedEntryParserResolver {
  fun resolve(document: Document): FeedEntryParser {
    return when {
      document.select("item").isNotEmpty() -> RssItemEntryParser()
      document.select("entry").isNotEmpty() -> AtomEntryParser()
      else -> throw IllegalArgumentException("지원하지 않는 피드 형식")
    }
  }
}
