package com.techinsights.domain.service.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.genai.Client
import com.google.genai.types.GenerateContentConfig
import com.techinsights.domain.config.gemini.GeminiProperties
import com.techinsights.domain.dto.gemini.SummaryResult
import com.techinsights.domain.enums.Category
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.stereotype.Service

@Service
class GeminiArticleSummarizer (
  private val geminiClient: Client,
  private val geminiProperties: GeminiProperties,
  rateLimiterRegistry: RateLimiterRegistry,
  private val promptTemplateEngine: PromptTemplateEngine
) : ArticleSummarizer {

  private val mapper  = jacksonObjectMapper()

  private val rateLimiter = rateLimiterRegistry.rateLimiter("geminiArticleSummarizer")

  override fun summarize(article: String): SummaryResult {
    val prompt  = promptTemplateEngine.buildPrompt(article, Category.entries.map { it.name })
    val schema  = promptTemplateEngine.buildSchema(Category.entries.map { it.name })
    val config  = GenerateContentConfig.builder()
      .responseJsonSchema(schema)
      .responseMimeType("application/json")
      .maxOutputTokens(256)
      .build()

    val response = rateLimiter.executeCallable {
      geminiClient.models.generateContent(geminiProperties.model, prompt, config)
    }
    return mapper.readValue(response.text(), SummaryResult::class.java)
  }

}