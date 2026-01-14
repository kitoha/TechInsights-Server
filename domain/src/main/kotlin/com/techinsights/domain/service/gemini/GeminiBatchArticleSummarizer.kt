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
        return summarizeBatchWithFallback(articles, modelType, depth = 0)
    }

    private suspend fun summarizeBatchWithFallback(
        articles: List<ArticleInput>,
        modelType: GeminiModelType,
        depth: Int
    ): BatchSummaryResponse {
        if (articles.isEmpty()) {
            return BatchSummaryResponse(emptyList())
        }

        if (articles.size == 1) {
            return summarizeSingle(articles.first(), modelType)
        }

        if (depth > MAX_FALLBACK_DEPTH) {
            log.warn("Max fallback depth reached, marking ${articles.size} articles as failed")
            return responseParser.createFailureResponse(articles, "Max retry depth exceeded")
        }

        log.info("Summarizing batch of ${articles.size} articles (depth=$depth)")

        return try {
            processBatchNormally(articles, modelType)

        } catch (e: Exception) {
            val errorType = classifyError(e)
            log.error("Batch processing failed with $errorType: ${e.message}")

            when {
                shouldFallbackToSplit(errorType, articles.size, depth) -> {
                    log.info("Applying binary search fallback for ${articles.size} articles")
                    binarySearchFallback(articles, modelType, depth)
                }
                else -> {
                    log.warn("Marking entire batch as failed (non-splittable error)")
                    responseParser.createFailureResponse(articles, e.message ?: "Batch failed")
                }
            }
        }
    }

    private suspend fun processBatchNormally(
        articles: List<ArticleInput>,
        modelType: GeminiModelType
    ): BatchSummaryResponse {
        val responseText = callGeminiApi(articles, modelType)
        val categories = Category.entries.map { it.name }.toSet()

        val parsedResponse = responseParser.parse(responseText, articles)
        return responseProcessor.process(parsedResponse, articles, categories)
    }

    private suspend fun summarizeSingle(
        article: ArticleInput,
        modelType: GeminiModelType
    ): BatchSummaryResponse {
        return try {
            processBatchNormally(listOf(article), modelType)
        } catch (e: Exception) {
            log.error("Individual processing failed for article ${article.id}: ${e.message}")
            responseParser.createFailureResponse(listOf(article), e.message ?: "Individual processing failed")
        }
    }

    private suspend fun binarySearchFallback(
        articles: List<ArticleInput>,
        modelType: GeminiModelType,
        depth: Int
    ): BatchSummaryResponse {
        val midpoint = articles.size / 2
        val firstHalf = articles.take(midpoint)
        val secondHalf = articles.drop(midpoint)

        log.debug("Splitting batch: ${articles.size} -> $midpoint + ${articles.size - midpoint}")

        val firstResults = summarizeBatchWithFallback(firstHalf, modelType, depth + 1)
        val secondResults = summarizeBatchWithFallback(secondHalf, modelType, depth + 1)

        return BatchSummaryResponse(firstResults.results + secondResults.results)
    }

    private fun classifyError(e: Exception): ErrorCategory {
        val message = e.message?.lowercase() ?: ""

        return when {
            message.contains("503") ||
            message.contains("overloaded") ||
            message.contains("timeout") ||
            message.contains("timed out") -> ErrorCategory.TRANSIENT_API

            message.contains("json") ||
            message.contains("parse") ||
            message.contains("truncat") ||
            message.contains("unexpected end") -> ErrorCategory.SIZE_RELATED

            message.contains("circuit") ||
            message.contains("breaker") -> ErrorCategory.CIRCUIT_OPEN

            else -> ErrorCategory.UNKNOWN
        }
    }

    private fun shouldFallbackToSplit(
        errorType: ErrorCategory,
        batchSize: Int,
        depth: Int
    ): Boolean {
        return when (errorType) {
            ErrorCategory.SIZE_RELATED -> batchSize > 1

            ErrorCategory.TRANSIENT_API -> batchSize > 3 && depth < 2

            ErrorCategory.CIRCUIT_OPEN -> false

            ErrorCategory.UNKNOWN -> batchSize > 2 && depth == 0
        }
    }

    private enum class ErrorCategory {
        SIZE_RELATED,
        TRANSIENT_API,
        CIRCUIT_OPEN,
        UNKNOWN
    }

    companion object {
        private const val MAX_FALLBACK_DEPTH = 4
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
