package com.techinsights.domain.service.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.genai.Client
import com.google.genai.types.GenerateContentConfig
import com.techinsights.domain.config.gemini.GeminiProperties
import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.BatchSummaryResponse
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import com.techinsights.domain.enums.Category
import com.techinsights.domain.enums.GeminiModelType
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GeminiBatchArticleSummarizer(
    private val geminiClient: Client,
    private val geminiProperties: GeminiProperties,
    private val promptBuilder: BatchPromptBuilder,
    private val validator: BatchSummaryValidator,
    rateLimiterRegistry: RateLimiterRegistry,
    circuitBreakerRegistry: CircuitBreakerRegistry
) : BatchArticleSummarizer {

    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = jacksonObjectMapper()

    private val rateLimiter = rateLimiterRegistry.rateLimiter("geminiBatchSummarizer")
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker("geminiBatch")

    override suspend fun summarizeBatch(
        articles: List<ArticleInput>,
        modelType: GeminiModelType
    ): BatchSummaryResponse {
        if (articles.isEmpty()) {
            return BatchSummaryResponse(emptyList())
        }

        log.info("Summarizing batch of ${articles.size} articles")

        val modelName = modelType.get()
        val categories = Category.entries.map { it.name }

        val prompt = promptBuilder.buildPrompt(articles, categories)
        val schema = promptBuilder.buildSchema(categories)
        val schemaNode = mapper.readTree(schema)

        val config = GenerateContentConfig.builder()
            .responseJsonSchema(schemaNode)
            .responseMimeType("application/json")
            .maxOutputTokens(geminiProperties.maxOutputTokens)
            .build()

        try {
            acquireRateLimiterPermission(rateLimiter)

            val response = withContext(Dispatchers.IO) {
                circuitBreaker.executeCallable {
                    geminiClient.models.generateContent(modelName, prompt, config)
                }
            }

            val responseText = response.text()

            if (responseText.isNullOrBlank()) {
                log.error("Received empty response from Gemini")
                return createFailureResponse(articles, "Empty response from API")
            }

            val batchResponse = mapper.readValue(responseText, BatchSummaryResponse::class.java)

            if (batchResponse.results.size != articles.size) {
                log.warn("Response count mismatch: expected ${articles.size}, got ${batchResponse.results.size}")
            }

            val validatedResults = batchResponse.results.map { result ->
                val input = articles.find { it.id == result.id }

                if (input == null) {
                    result.copy(success = false, error = "Unknown ID")
                } else {
                    val validation = validator.validate(input, result, categories.toSet())
                    if (!validation.isValid) {
                        result.copy(
                            success = false,
                            error = validation.errors.joinToString(", ")
                        )
                    } else {
                        result
                    }
                }
            }

            val receivedIds = validatedResults.map { it.id }.toSet()
            val missingResults = articles
                .filter { it.id !in receivedIds }
                .map {
                    SummaryResultWithId(
                        id = it.id,
                        success = false,
                        summary = null,
                        categories = null,
                        preview = null,
                        error = "No response received for this article"
                    )
                }

            return BatchSummaryResponse(validatedResults + missingResults)

        } catch (e: Exception) {
            log.error("Failed to process batch", e)
            return createFailureResponse(articles, e.message ?: "Unknown error")
        }
    }

    private suspend fun acquireRateLimiterPermission(rateLimiter: RateLimiter) {
        withContext(Dispatchers.IO) {
            rateLimiter.acquirePermission()
        }
    }

    private fun createFailureResponse(
        articles: List<ArticleInput>,
        errorMessage: String
    ): BatchSummaryResponse {
        val results = articles.map { article ->
            SummaryResultWithId(
                id = article.id,
                success = false,
                summary = null,
                categories = null,
                preview = null,
                error = errorMessage
            )
        }
        return BatchSummaryResponse(results)
    }
}
