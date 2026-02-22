package com.techinsights.domain.config.search

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "search.semantic")
data class SemanticSearchProperties(
    val defaultSize: Int = 10,
    val maxSize: Int = 20
) {
    init {
        require(defaultSize > 0) { "defaultSize must be positive, got $defaultSize" }
        require(maxSize >= defaultSize) { "maxSize($maxSize) must be >= defaultSize($defaultSize)" }
    }
}
