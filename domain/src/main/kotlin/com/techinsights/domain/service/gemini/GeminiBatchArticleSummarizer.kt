package com.techinsights.domain.service.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.genai.Client
import com.google.genai.types.GenerateContentConfig
import com.techinsights.domain.config.gemini.GeminiProperties
import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.BatchSummaryResponse
import com.techinsights.domain.enums.Category
import com.techinsights.domain.enums.GeminiModelType
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.coroutines.CoroutineContext

@Service
class GeminiBatchArticleSummarizer(
    private val geminiClient: Client,
    private val geminiProperties: GeminiProperties,
    private val promptBuilder: BatchPromptBuilder,
    private val responseParser: BatchResponseParser,
    private val responseProcessor: BatchResponseProcessor,
    rateLimiterRegistry: RateLimiterRegistry,
    circuitBreakerRegistry: CircuitBreakerRegistry,
    private val ioDispatcher: CoroutineContext = Dispatchers.IO
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

        return try {
            val responseText = callGeminiApi(articles, modelType)
            val categories = Category.entries.map { it.name }.toSet()

            val parsedResponse = responseParser.parse(responseText, articles)
            responseProcessor.process(parsedResponse, articles, categories)

        } catch (e: Exception) {
            log.error("Failed to process batch", e)
            responseParser.createFailureResponse(articles, e.message ?: "Unknown error")
        }
    }

    private suspend fun callGeminiApi(
        articles: List<ArticleInput>,
        modelType: GeminiModelType
    ): String {
        val modelName = modelType.get()
        val categories = Category.entries.map { it.name }

        val prompt = promptBuilder.buildPrompt(articles, categories)
        val config = buildGeminiConfig(categories)

        acquireRateLimiterPermission()

        val response = withContext(ioDispatcher) {
            circuitBreaker.executeCallable {
                geminiClient.models.generateContent(modelName, prompt, config)
            }
        }

        return response.text() ?: ""
    }

    private fun buildGeminiConfig(categories: List<String>): GenerateContentConfig {
        val schema = promptBuilder.buildSchema(categories)
        val schemaNode = mapper.readTree(schema)

        return GenerateContentConfig.builder()
            .responseJsonSchema(schemaNode)
            .responseMimeType("application/json")
            .maxOutputTokens(geminiProperties.maxOutputTokens)
            .build()
    }

    private suspend fun acquireRateLimiterPermission() {
        withContext(ioDispatcher) {
            rateLimiter.acquirePermission()
        }
    }
}
