package com.techinsights.batch.parser.feed

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

interface FeedTypeStrategy {

  fun supports(document: Document): Boolean
  fun parseEntries(document: Document): List<Element>
  fun extractLink(element: Element): String
  fun extractPublishedDate(element: Element): String
  fun extractContent(element: Element): String
}