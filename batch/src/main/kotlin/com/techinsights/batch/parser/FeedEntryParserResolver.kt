package com.techinsights.batch.parser

import org.w3c.dom.Document

object FeedEntryParserResolver {
  fun resolve(document: Document): FeedEntryParser {
    return when {
      document.getElementsByTagName("item").length > 0 -> RssItemEntryParser()
      document.getElementsByTagName("entry").length > 0 -> AtomEntryParser()
      else -> throw IllegalArgumentException("지원하지 않는 피드 형식")
    }
  }
}
