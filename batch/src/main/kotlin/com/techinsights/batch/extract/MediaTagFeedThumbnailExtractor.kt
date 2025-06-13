package com.techinsights.batch.extract

import org.springframework.stereotype.Component
import org.w3c.dom.Element

@Component
class MediaTagFeedThumbnailExtractor : ThumbnailExtractorStrategy {
  override fun extract(context: Any): String? {
    if (context !is Element) return null
    return context.getElementsByTagName("media:thumbnail")
      .item(0)
      ?.attributes?.getNamedItem("url")
      ?.nodeValue
  }
}