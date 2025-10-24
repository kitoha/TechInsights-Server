package com.techinsights.batch.parser.content

import com.techinsights.batch.config.props.ContentSelectorProperties
import org.springframework.stereotype.Component

@Component
class ContentSelectorRegistry(
  private val properties: ContentSelectorProperties
) {
  fun getSelectors(domain: String): List<String> =
    properties.selectors[domain] ?: properties.defaultSelectors
}
