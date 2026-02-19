package com.techinsights.batch.crawling.extract

import org.jsoup.nodes.Document
import org.springframework.stereotype.Component

@Component
class PictureSourceHtmlThumbnailExtractor : ThumbnailExtractorStrategy {
  override fun extract(context: Any): String? {
    if (context !is Document) return null
    return context.select("picture source[srcset], figure.paragraph-image picture source[srcset]")
      .mapNotNull { it.attr("srcset").split(",").lastOrNull()?.trim()?.split(" ")?.firstOrNull() }
      .firstOrNull { it.isNotBlank() }
      ?.let { srcSet ->
        org.jsoup.Jsoup.parse("<img src='$srcSet'/>", context.baseUri())
          .selectFirst("img")?.absUrl("src") ?: srcSet
      }
  }
}