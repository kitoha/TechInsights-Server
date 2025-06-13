package com.techinsights.batch.extract

interface ThumbnailExtractorStrategy {
  fun extract(context: Any): String?
}