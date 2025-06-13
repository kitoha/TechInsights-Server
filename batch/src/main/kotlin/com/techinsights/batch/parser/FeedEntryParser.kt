package com.techinsights.batch.parser

import org.w3c.dom.Document
import org.w3c.dom.Element

interface FeedEntryParser {
  fun parseEntries(document: Document): List<Element>
}