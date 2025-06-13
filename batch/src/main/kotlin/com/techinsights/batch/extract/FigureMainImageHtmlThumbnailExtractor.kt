package com.techinsights.batch.extract

import org.jsoup.nodes.Document
import org.springframework.stereotype.Component

@Component
class FigureMainImageHtmlThumbnailExtractor : ThumbnailExtractorStrategy {
  override fun extract(context: Any): String? {
    if (context !is Document) return null
    return context.select("figure.paragraph-image picture img, figure.paragraph-image img")
      .firstOrNull { isValidImg(it) }
      ?.absUrl("src")
  }
  private fun isValidImg(img: org.jsoup.nodes.Element): Boolean =
    !img.absUrl("src").isNullOrBlank() &&
        !img.absUrl("src").contains("data:image/svg") &&
        !img.absUrl("src").contains("icon") &&
        !img.absUrl("src").endsWith(".svg")
}