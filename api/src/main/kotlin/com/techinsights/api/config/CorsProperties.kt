package com.techinsights.api.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "cors")
data class CorsProperties(
  var allowedOrigins: List<String> = emptyList()
)