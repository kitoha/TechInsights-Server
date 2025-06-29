package com.techinsights.domain.config.gemini

import com.google.genai.Client
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GeminiConfig(
  private val geminiProperties: GeminiProperties,
) {

  @Bean
  fun geminiClient(): Client {
    return if(geminiProperties.apiKey.isNotBlank()) {
      Client.builder()
        .apiKey(geminiProperties.apiKey)
        .build()
    } else {
      throw IllegalArgumentException("Gemini API key must be provided")
    }
  }

}