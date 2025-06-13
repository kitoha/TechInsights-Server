package com.techinsights.batch.extract

import org.springframework.stereotype.Component
import org.w3c.dom.Element

@Component
class ImageTagFeedThumbnailExtractor : ThumbnailExtractorStrategy {
  override fun extract(context: Any): String? {
    if (context !is Element) return null
    return context.getElementsByTagName("image")
      .item(0)
      ?.textContent
      ?.takeIf { !it.isNullOrBlank() }
  }
}