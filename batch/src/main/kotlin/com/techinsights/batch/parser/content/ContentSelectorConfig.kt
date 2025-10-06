package com.techinsights.batch.parser.content

data class ContentSelectorConfig(
  val domain: String,
  val selectors: List<String>
)