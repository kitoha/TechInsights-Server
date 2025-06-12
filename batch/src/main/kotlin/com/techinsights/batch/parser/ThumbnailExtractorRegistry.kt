package com.techinsights.batch.parser

object ThumbnailExtractorRegistry {
  private val extractorMap: Map<Regex, FeedThumbnailExtractor> = mapOf(
    Regex(".*") to FeedThumbnailExtractor()
  )

  fun extractorFor(feedUrl: String): FeedThumbnailExtractor {
    return extractorMap.entries.find { feedUrl.matches(it.key) }?.value
      ?: FeedThumbnailExtractor()
  }
}