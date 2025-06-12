package com.techinsights.batch.parser

import org.w3c.dom.Element

interface ThumbnailExtractor {
  fun extractThumbnail(itemElement: Element): String?
}