package com.techinsights.batch.parser.feed

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class RssFeedStrategy : FeedTypeStrategy {

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
        val decodedHtml = org.jsoup.parser.Parser.unescapeEntities(escapedHtml, false)
        return extractStructuredText(decodedHtml)
      }
    }
    return ""
  }

  private fun extractStructuredText(html: String): String {
    val doc = Jsoup.parse(html)
    val body = doc.body()

    val sb = StringBuilder()
    traverseAndExtract(body, sb)

    return sb.toString()
      .replace(Regex("\n{3,}"), "\n\n")
      .trim()
  }

  private fun traverseAndExtract(element: Element, sb: StringBuilder) {
    for (child in element.children()) {
      when (child.tagName()) {
        "h1" -> sb.append("\n# ${child.text().trim()}\n\n")
        "h2" -> sb.append("\n## ${child.text().trim()}\n\n")
        "h3" -> sb.append("\n### ${child.text().trim()}\n\n")
        "h4" -> sb.append("\n#### ${child.text().trim()}\n\n")
        "p" -> {
          val text = child.text().trim()
          if (text.isNotBlank()) {
            sb.append("${text}\n\n")
          }
        }

        "li" -> sb.append("- ${child.text().trim()}\n")
        "blockquote" -> {
          val text = child.text().trim()
          if (text.isNotBlank()) {
            sb.append("> ${text}\n\n")
          }
        }

        "a" -> {
          val text = child.text().trim()
          val href = child.attr("href")
          if (text.isNotBlank()) {
            sb.append("[$text]($href) ")
          }
        }

        "img" -> {}
        "ol", "ul", "div", "section", "article" -> traverseAndExtract(child, sb)
        else -> {
          if (child.children().isEmpty()) {
            val text = child.text().trim()
            if (text.isNotBlank()) {
              sb.append("${text} ")
            }
          } else {
            traverseAndExtract(child, sb)
          }
        }
      }
    }
  }
}
