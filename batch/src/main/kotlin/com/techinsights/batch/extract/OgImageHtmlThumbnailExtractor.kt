package com.techinsights.batch.extract

import org.jsoup.nodes.Document
import org.springframework.stereotype.Component

@Component
class OgImageHtmlThumbnailExtractor : ThumbnailExtractorStrategy {
  override fun extract(context: Any): String? {
    if (context !is Document) return null
    return context.selectFirst("meta[property=og:image]")
      ?.attr("content")
      ?.takeIf { it.isNotBlank() }
  }
}