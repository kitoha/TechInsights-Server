package com.techinsights.domain.service.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.genai.Client
import com.google.genai.types.GenerateContentConfig
import com.techinsights.domain.config.gemini.GeminiProperties
import com.techinsights.domain.dto.gemini.SummaryResult
import com.techinsights.domain.enums.Category
import com.techinsights.domain.enums.GeminiModelType
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GeminiArticleSummarizer (
  private val geminiClient: Client,
  private val geminiProperties: GeminiProperties,
  rateLimiterRegistry: RateLimiterRegistry,
  private val promptTemplateEngine: PromptTemplateEngine
) : ArticleSummarizer {

  private val log = LoggerFactory.getLogger(GeminiArticleSummarizer::class.java)

  private val mapper  = jacksonObjectMapper()

  private val rateLimiter = rateLimiterRegistry.rateLimiter("geminiArticleSummarizer")

  override fun summarize(article: String, modelType: GeminiModelType): SummaryResult {
    if (article.isBlank()) {
      log.warn("Article is blank, skipping summarization.")
      throw IllegalArgumentException("Article content cannot be blank.")
    }

    val modelName = modelType.getModelName()

    val prompt  = promptTemplateEngine.buildPrompt(article, Category.entries.map { it.name })
    val schema  = promptTemplateEngine.buildSchema(Category.entries.map { it.name })
    val schemaNode = mapper.readTree(schema)
    val config  = GenerateContentConfig.builder()
      .responseJsonSchema(schemaNode)
      .responseMimeType("application/json")
      .maxOutputTokens(geminiProperties.maxOutputTokens)
      .build()

    try {
      val response = rateLimiter.executeCallable {
        geminiClient.models.generateContent(modelName, prompt, config)
      }
      return mapper.readValue(response.text(), SummaryResult::class.java)
    } catch (e: Exception) {
      log.error("Failed to summarize article with Gemini model: $modelName", e)
      throw RuntimeException("Failed to summarize article with Gemini model: $modelName", e)
    }
  }

}