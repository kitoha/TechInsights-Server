package com.techinsights.domain.config.gemini

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("gemini")
class GeminiProperties {
  var apiKey: String = ""
  var model: String = "gemini-2.0-flash-001"
}