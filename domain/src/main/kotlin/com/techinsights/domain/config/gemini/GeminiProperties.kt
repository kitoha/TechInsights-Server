package com.techinsights.domain.config.gemini

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("gemini")
data class GeminiProperties(
    var apiKey: String = "",
    var maxOutputTokens: Int = 16384  // Increased from 8192 to support larger batches (RPD optimization)
)