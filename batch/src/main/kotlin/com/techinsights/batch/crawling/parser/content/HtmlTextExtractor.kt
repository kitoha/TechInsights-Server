package com.techinsights.batch.crawling.parser.content

import org.jsoup.nodes.Element

class HtmlTextExtractor {

  fun extract(element: Element): String {
    val builder = StringBuilder()
    traverse(element, builder)
    return builder.toString()
      .replace(Regex("\n{3,}"), "\n\n")
      .trim()
  }

  private fun traverse(node: Element, builder: StringBuilder) {
    for (child in node.children()) {
      when (child.tagName()) {
        "h1" -> builder.append("\n# ${child.text().trim()}\n")
        "h2" -> builder.append("\n## ${child.text().trim()}\n")
        "h3" -> builder.append("\n### ${child.text().trim()}\n")
        "h4" -> builder.append("\n#### ${child.text().trim()}\n")
        "li" -> builder.append("- ${child.text().trim()}\n")
        "p" -> builder.append("${child.text().trim()}\n")
        "ol", "ul", "div", "section", "article" -> traverse(child, builder)
        else -> traverse(child, builder)
      }
    }
  }
}