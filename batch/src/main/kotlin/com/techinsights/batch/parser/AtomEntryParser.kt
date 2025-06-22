package com.techinsights.batch.parser

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element


class AtomEntryParser : FeedEntryParser {
  override fun parseEntries(document: Document): List<Element> {
    return document.select("entry")
  }
}
