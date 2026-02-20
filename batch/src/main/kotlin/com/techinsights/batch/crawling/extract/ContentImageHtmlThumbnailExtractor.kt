package com.techinsights.batch.crawling.extract

import org.jsoup.nodes.Document
import org.springframework.stereotype.Component

@Component
class ContentImageHtmlThumbnailExtractor : ThumbnailExtractorStrategy {

  override fun extract(context: Any): String? {
    if (context !is Document) return null

    val contentSelectors = listOf(
      ".con_view img",
      "article img",
      ".post-content img",
      ".entry-content img",
      "main img",
      ".content img"
    )

    for (selector in contentSelectors) {
      val img = context.select(selector)
        .firstOrNull { isValidThumbnail(it) }

      if (img != null) {
        return img.absUrl("src")
      }
    }

    return null
  }

  private fun isValidThumbnail(img: org.jsoup.nodes.Element): Boolean {
    val src = img.absUrl("src")

    if (src.isBlank()) return false
    if (src.contains("data:image/")) return false
    if (src.endsWith(".svg")) return false
    if (src.contains("/icon")) return false
    if (src.contains("/logo")) return false

    val width = img.attr("width").toIntOrNull()
    val height = img.attr("height").toIntOrNull()

    if (width != null && width < 100) return false
    if (height != null && height < 100) return false

    return true
  }
}