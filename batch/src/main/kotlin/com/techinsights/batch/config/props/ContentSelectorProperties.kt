package com.techinsights.batch.config.props

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "content")
data class ContentSelectorProperties(
  var selectors: Map<String, List<String>> = emptyMap(),
  var defaultSelectors: List<String> = emptyList()
)