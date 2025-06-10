package com.techinsights.parser

import org.w3c.dom.Element

interface ThumbnailExtractor {
  fun extractThumbnail(itemElement: Element): String?
}