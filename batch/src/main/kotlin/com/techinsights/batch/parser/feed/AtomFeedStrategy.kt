package com.techinsights.batch.parser.feed

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

class AtomFeedStrategy : FeedTypeStrategy {

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

        "img" -> {} // 이미지는 무시
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