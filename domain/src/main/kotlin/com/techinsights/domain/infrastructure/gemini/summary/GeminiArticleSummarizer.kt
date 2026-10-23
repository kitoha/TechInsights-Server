package com.techinsights.domain.infrastructure.gemini.summary

import com.techinsights.domain.infrastructure.gemini.prompt.PromptTemplateEngine
import com.techinsights.domain.service.summary.ArticleSummarizer
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.genai.Client
import com.google.genai.types.GenerateContentConfig
import com.techinsights.domain.infrastructure.gemini.GeminiProperties
import com.techinsights.domain.dto.summary.SummaryResult
import com.techinsights.domain.enums.Category
import com.techinsights.domain.infrastructure.gemini.GeminiModelType
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GeminiArticleSummarizer(
  private val geminiClient: Client,
  private val geminiProperties: GeminiProperties,
  private val mapper: ObjectMapper,
  rateLimiterRegistry: RateLimiterRegistry,
  private val promptTemplateEngine: PromptTemplateEngine,
) : ArticleSummarizer {

  private val log = LoggerFactory.getLogger(GeminiArticleSummarizer::class.java)

  private val rateLimiter = rateLimiterRegistry.rateLimiter("geminiArticleSummarizer")

  override fun summarize(article: String): SummaryResult {
    if (article.isBlank()) {
      log.warn("Article is blank, skipping summarization.")
      throw IllegalArgumentException("Article content cannot be blank.")
    }

    val modelName = MODEL.get()

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

      val responseText = response.text()

      if (responseText.isNullOrBlank()) {
        log.error("Received empty response from Gemini model: $modelName")
        throw RuntimeException("Received empty response from Gemini model: $modelName")
      }

      return mapper.readValue(response.text(), SummaryResult::class.java)
    } catch (e: Exception) {
      log.error("Failed to summarize article with Gemini model: $modelName", e)
      throw RuntimeException("Failed to summarize article with Gemini model: $modelName", e)
    }
  }

  companion object {
    private val MODEL = GeminiModelType.GEMINI_2_5_FLASH_LITE
  }
}
