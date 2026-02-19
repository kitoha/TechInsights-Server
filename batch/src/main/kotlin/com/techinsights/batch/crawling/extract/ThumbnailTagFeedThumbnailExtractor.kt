package com.techinsights.batch.crawling.extract

import org.jsoup.nodes.Element
import org.springframework.stereotype.Component


@Component
class ThumbnailTagFeedThumbnailExtractor : ThumbnailExtractorStrategy {
  override fun extract(context: Any): String? {
    if (context !is Element) return null
    return context.selectFirst("thumbnail")
      ?.text()
      ?.takeIf { it.isNotBlank() }
  }
}