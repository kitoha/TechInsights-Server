package com.techinsights.batch.extract

fun interface ThumbnailExtractorStrategy {
  fun extract(context: Any): String?
}