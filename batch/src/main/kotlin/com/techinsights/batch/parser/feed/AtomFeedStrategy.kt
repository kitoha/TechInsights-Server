package com.techinsights.batch.parser.feed

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

class AtomFeedStrategy : BaseFeedStrategy() {

  override fun supports(document: Document): Boolean =
    document.selectFirst("feed[xmlns*=atom]") != null

  override fun parseEntries(document: Document): List<Element> =
    document.select("entry")

  override fun extractLink(element: Element): String =
    element.selectFirst("link[rel=alternate]")?.attr("abs:href")
      ?: element.selectFirst("link")?.attr("abs:href")
      ?: element.selectFirst("id")?.text()
      ?: ""

  override fun extractPublishedDate(element: Element): String =
    element.selectFirst("updated")?.text()
      ?: element.selectFirst("published")?.text()
      ?: ""

  override fun extractContent(element: Element): String {
    val contentElement = element.children()
      .firstOrNull { it.tagName() == "content" }

    if (contentElement != null) {
      val escapedHtml = contentElement.html()
      if (escapedHtml.isNotBlank()) {
        val decodedHtml = Parser.unescapeEntities(escapedHtml, false)
        return extractStructuredText(decodedHtml)
      }
    }

    val summaryElement = element.children()
      .firstOrNull { it.tagName() == "summary" }
    return summaryElement?.text() ?: ""
  }
}