package com.techinsights.batch.crawling.parser.feed.handler

import org.jsoup.nodes.Element

sealed class TagHandler {

  abstract fun handle(element: Element, sb: StringBuilder)

  data class Header(val level: Int) : TagHandler() {

    override fun handle(element: Element, sb: StringBuilder) {
      val prefix = "#".repeat(level)
      sb.append("\n$prefix ${element.text().trim()}\n\n")
    }
  }

  data object Paragraph : TagHandler() {

    override fun handle(element: Element, sb: StringBuilder) {
      val text = element.text().trim()
      if (text.isNotBlank()) {
        sb.append("$text\n\n")
      }
    }
  }

  data object ListItem : TagHandler() {

    override fun handle(element: Element, sb: StringBuilder) {
      sb.append("- ${element.text().trim()}\n")
    }
  }

  data object Blockquote : TagHandler() {

    override fun handle(element: Element, sb: StringBuilder) {
      val text = element.text().trim()
      if (text.isNotBlank()) {
        sb.append("> $text\n\n")
      }
    }
  }

  data object Link : TagHandler() {

    override fun handle(element: Element, sb: StringBuilder) {
      val text = element.text().trim()
      if (text.isNotBlank()) {
        val href = element.attr("href")
        sb.append("[$text]($href) ")
      }
    }
  }

  data object Default : TagHandler() {

    override fun handle(element: Element, sb: StringBuilder) {
      if (element.children().isEmpty()) {
        val text = element.text().trim()
        if (text.isNotBlank()) {
          sb.append("$text ")
        }
      }
    }
  }
}
