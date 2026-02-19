package com.techinsights.batch.crawling.parser.feed

import com.techinsights.batch.crawling.parser.feed.handler.TagHandler
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

abstract class BaseFeedStrategy : FeedTypeStrategy {

  private val containerTags = setOf("ol", "ul", "div", "section", "article")

  private val tagHandlers = mapOf(
    "h1" to TagHandler.Header(1),
    "h2" to TagHandler.Header(2),
    "h3" to TagHandler.Header(3),
    "h4" to TagHandler.Header(4),
    "h5" to TagHandler.Header(5),
    "h6" to TagHandler.Header(6),
    "p" to TagHandler.Paragraph,
    "li" to TagHandler.ListItem,
    "blockquote" to TagHandler.Blockquote,
    "a" to TagHandler.Link
  )

  protected fun extractStructuredText(html: String): String {
    val doc = Jsoup.parse(html)
    val body = doc.body()
    val sb = StringBuilder()

    traverseAndExtract(body, sb)

    return sb.toString()
      .replace(Regex("\n{3,}"), "\n\n")
      .trim()
  }

  private fun traverseAndExtract(element: Element, sb: StringBuilder) {
    element.children().forEach { child ->
      val tagName = child.tagName()

      when {
        containerTags.contains(tagName) -> traverseAndExtract(child, sb)
        tagHandlers.containsKey(tagName) -> tagHandlers[tagName]?.handle(child, sb)
        else -> {
          if (child.children().isEmpty()) {
            TagHandler.Default.handle(child, sb)
          } else {
            traverseAndExtract(child, sb)
          }
        }
      }
    }
  }
}