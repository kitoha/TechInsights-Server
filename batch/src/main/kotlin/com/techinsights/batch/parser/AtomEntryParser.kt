package com.techinsights.batch.parser

import org.w3c.dom.Document
import org.w3c.dom.Element

class AtomEntryParser : FeedEntryParser {
  override fun parseEntries(document: Document): List<Element> {
    val entries = document.getElementsByTagName("entry")
    return (0 until entries.length)
      .map { entries.item(it) as Element }
  }
}
