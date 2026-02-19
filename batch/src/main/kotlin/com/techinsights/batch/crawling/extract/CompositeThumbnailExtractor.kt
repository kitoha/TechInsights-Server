package com.techinsights.batch.crawling.extract

import org.springframework.stereotype.Component

@Component
class CompositeThumbnailExtractor(
  private val strategies: List<ThumbnailExtractorStrategy>
) {
  fun extract(context: Any): String? =
    strategies.firstNotNullOfOrNull { it.extract(context) }
}
