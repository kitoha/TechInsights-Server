package com.techinsights.batch.crawling.extract

fun interface ThumbnailExtractorStrategy {
  fun extract(context: Any): String?
}