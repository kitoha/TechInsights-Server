package com.techinsights.batch.parser

import org.w3c.dom.Element

class FeedThumbnailExtractor : ThumbnailExtractor {

  override fun extractThumbnail(item: Element): String? {
    item.getElementsByTagName("media:thumbnail").item(0)?.attributes?.getNamedItem("url")?.nodeValue?.let {
      return it
    }
    item.getElementsByTagName("thumbnail").item(0)?.textContent?.let {
      if (it.isNotBlank()) return it
    }
    item.getElementsByTagName("image").item(0)?.textContent?.let {
      if (it.isNotBlank()) return it
    }
    return null
  }
}