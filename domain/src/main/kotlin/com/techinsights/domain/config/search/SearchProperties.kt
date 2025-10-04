package com.techinsights.domain.config.search

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "search")
data class SearchProperties(
  val score: ScoreProperties = ScoreProperties(),
  val instant: InstantSearchProperties = InstantSearchProperties(),
  val full: FullSearchProperties = FullSearchProperties()
)