package com.techinsights.crawling

import org.w3c.dom.Element

interface ThumbnailExtractor {
  fun extractThumbnail(itemElement: Element): String?
}