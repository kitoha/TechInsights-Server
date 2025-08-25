package com.techinsights.domain.config.gemini

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("gemini")
data class GeminiProperties(
    var apiKey: String = "",
    var maxOutputTokens: Int = 4096
)