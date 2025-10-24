package com.techinsights.batch.parser

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element


fun interface FeedEntryParser {
  fun parseEntries(document: Document): List<Element>
}