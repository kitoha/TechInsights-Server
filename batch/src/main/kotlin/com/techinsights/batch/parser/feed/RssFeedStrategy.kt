package com.techinsights.batch.parser.feed

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

class RssFeedStrategy : BaseFeedStrategy() {

  override fun supports(document: Document): Boolean =
    document.selectFirst("rss") != null || document.selectFirst("channel") != null

  override fun parseEntries(document: Document): List<Element> =
    document.select("item")

  override fun extractLink(element: Element): String =
    element.selectFirst("link")?.text()
      ?: element.selectFirst("guid")?.text()
      ?: ""

  override fun extractPublishedDate(element: Element): String =
    element.selectFirst("pubDate")?.text()
      ?: element.selectFirst("dc|date")?.text()
      ?: ""

  override fun extractContent(element: Element): String {
    val contentElement = element.children()
      .firstOrNull { it.tagName() == "content:encoded" || it.tagName() == "encoded" }
      ?: element.children().firstOrNull { it.tagName() == "description" }

    if (contentElement != null) {
      val escapedHtml = contentElement.html()
      if (escapedHtml.isNotBlank()) {
        val decodedHtml = Parser.unescapeEntities(escapedHtml, false)
        return extractStructuredText(decodedHtml)
      }
    }
    return ""
  }
}