package com.techinsights.batch.extract

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component

@Component
class FeedContentImageExtractor : ThumbnailExtractorStrategy {

  override fun extract(context: Any): String? {
    if (context !is Element) return null

    val contentElement = context.children()
      .firstOrNull { it.tagName() == "content" || it.tagName() == "description" }
      ?: return null

    val html = contentElement.html()
    if (html.isBlank()) return null

    val decodedHtml = org.jsoup.parser.Parser.unescapeEntities(html, false)

    val doc = Jsoup.parse(decodedHtml)
    val img = doc.select("img")
      .firstOrNull { isValidImage(it) }
      ?: return null

    val src = img.attr("src")
    return when {
      src.startsWith("http://") || src.startsWith("https://") -> src
      src.startsWith("/") -> extractBaseUrl(context) + src
      else -> src
    }
  }

  private fun isValidImage(img: Element): Boolean {
    val src = img.attr("src")

    if (src.isBlank()) return false

    if (src.contains("data:image/")) return false

    if (src.endsWith(".svg")) return false

    if (src.contains("/icon")) return false

    if (src.contains("/logo")) return false

    return true
  }

  private fun extractBaseUrl(element: Element): String {
    val link = element.selectFirst("link")?.attr("href")
      ?: element.selectFirst("link")?.attr("abs:href")
      ?: ""

    if (link.isBlank()) return ""

    return try {
      val url = java.net.URI(link)
      "${url.scheme}://${url.host}"
    } catch (e: Exception) {
      ""
    }
  }
}