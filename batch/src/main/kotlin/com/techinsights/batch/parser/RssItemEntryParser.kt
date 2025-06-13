package com.techinsights.batch.parser

import org.w3c.dom.Document
import org.w3c.dom.Element

class RssItemEntryParser : FeedEntryParser {
  override fun parseEntries(document: Document): List<Element> {
    val items = document.getElementsByTagName("item")
    return (0 until items.length)
      .map { items.item(it) as Element }
  }
}