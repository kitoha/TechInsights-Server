package com.techinsights.batch.extract

import org.jsoup.nodes.Element
import org.springframework.stereotype.Component

@Component
class MediaTagFeedThumbnailExtractor : ThumbnailExtractorStrategy {
  override fun extract(context: Any): String? {
    if (context !is Element) return null
    return context.selectFirst("media|thumbnail")
      ?.attr("url")
      ?.takeIf { it.isNotBlank() }
  }
}