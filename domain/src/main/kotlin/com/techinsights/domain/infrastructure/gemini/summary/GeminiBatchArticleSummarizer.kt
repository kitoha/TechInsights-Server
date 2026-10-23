package com.techinsights.domain.infrastructure.gemini.summary

import com.techinsights.domain.infrastructure.gemini.parser.StreamingJsonParser
import com.techinsights.domain.infrastructure.gemini.prompt.BatchPromptBuilder
import com.techinsights.domain.service.summary.BatchArticleSummarizer
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.genai.Client
import com.google.genai.types.GenerateContentConfig
import com.techinsights.domain.infrastructure.gemini.GeminiProperties
import com.techinsights.domain.dto.summary.ArticleInput
import com.techinsights.domain.dto.summary.SummaryResultWithId
import com.techinsights.domain.enums.Category
import com.techinsights.domain.enums.ErrorType
import com.techinsights.domain.infrastructure.gemini.GeminiModelType
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.techinsights.domain.service.quota.DailyQuota
import com.techinsights.domain.service.quota.DailyQuotaService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.coroutines.CoroutineContext

@Service
class GeminiBatchArticleSummarizer(
    private val geminiClient: Client,
    private val geminiProperties: GeminiProperties,
    private val promptBuilder: BatchPromptBuilder,
    private val mapper: ObjectMapper,
    rateLimiterRegistry: RateLimiterRegistry,
    circuitBreakerRegistry: CircuitBreakerRegistry,
    private val dailyQuotaService: DailyQuotaService,
    private val ioDispatcher: CoroutineContext = Dispatchers.IO
) : BatchArticleSummarizer {

    private val log = LoggerFactory.getLogger(javaClass)

    private val rpmLimiter = rateLimiterRegistry.rateLimiter("geminiBatchRpm")
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker("geminiBatch")

    override fun summarizeBatch(articles: List<ArticleInput>): Flow<SummaryResultWithId> {
        return channelFlow {
            val results = mutableSetOf<String>()
            var lastErrorType: ErrorType? = null
            var lastErrorMessage: String? = null
            
            callGeminiApi(articles).collect { result ->
                if (result.id == "SYSTEM_ERROR_MARKER") {
                    lastErrorType = result.errorType
                    lastErrorMessage = result.error
                } else {
                    results.add(result.id)
                    if (!result.success) {
                        lastErrorType = result.errorType
                        lastErrorMessage = result.error
                    }
                    send(result)
                }
            }

            articles.filter { it.id !in results }.forEach { missing ->
                val errorType = lastErrorType ?: ErrorType.CONTENT_ERROR
                val errorMessage = lastErrorMessage ?: "Response missing from Gemini stream (possible parsing or model error)"
                
                log.warn("Post ${missing.id} missing from Gemini stream response. Marking as failed with type $errorType. Reason: $errorMessage")
                send(
                    SummaryResultWithId(
                        id = missing.id,
                        success = false,
                        error = errorMessage,
                        errorType = errorType
                    )
                )
            }
        }.buffer(Channel.UNLIMITED)
    }

    private fun callGeminiApi(
        articles: List<ArticleInput>
    ): Flow<SummaryResultWithId> = channelFlow {
        val modelName = MODEL.get()
        val categories = Category.entries.map { it.name }

        val prompt = promptBuilder.buildPrompt(articles, categories)
        val config = buildGeminiConfig(categories)

        if (!dailyQuotaService.tryReserve(DailyQuota.BATCH_SUMMARY)) {
            log.warn("Daily Gemini quota exhausted for article summary. Articles: ${articles.map { it.id }}")
            articles.forEach { article ->
                send(SummaryResultWithId(article.id, false, error = "Daily Gemini quota exhausted", errorType = ErrorType.RATE_LIMIT))
            }
            return@channelFlow
        }
        acquireRateLimiterPermission(rpmLimiter)

        try {
            val responseStream = circuitBreaker.executeCallable {
                geminiClient.models.generateContentStream(modelName, prompt, config)
            }

            val jsonParser = StreamingJsonParser(SummaryResultWithId::class.java) { it.id }
            for (res in responseStream) {
                val candidates = res.candidates().orElse(null)
                val finishReason = candidates?.firstOrNull()?.finishReason()
                val currentErrorType = when (finishReason?.toString()?.uppercase()) {
                    "SAFETY" -> ErrorType.SAFETY_BLOCKED
                    "MAX_TOKENS" -> ErrorType.LENGTH_LIMIT
                    "OTHER" -> ErrorType.API_ERROR
                    else -> null
                }

                if (currentErrorType != null) {
                    log.warn("Gemini stream interrupted. Reason: $finishReason, Articles: ${articles.map { it.id }}")
                    send(SummaryResultWithId(
                        id = "SYSTEM_ERROR_MARKER",
                        success = false,
                        error = "Gemini finished with reason: $finishReason",
                        errorType = currentErrorType
                    ))
                }

                res.text()?.let { textChunk ->
                    jsonParser.process(textChunk).forEach { summary ->
                        launch(ioDispatcher) { send(summary) }
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Streaming summarization failed for articles [${articles.map { it.id }.joinToString()}].", e)
            articles.forEach { article ->
                launch(ioDispatcher) {
                    send(SummaryResultWithId(article.id, false, error = e.message ?: "Streaming failed", errorType = ErrorType.API_ERROR))
                }
            }
        }
    }.buffer(Channel.UNLIMITED)

    private fun buildGeminiConfig(categories: List<String>): GenerateContentConfig {
        val schema = promptBuilder.buildSchema(categories)
        val schemaNode = mapper.readTree(schema)

        return GenerateContentConfig.builder()
            .responseJsonSchema(schemaNode)
            .responseMimeType("application/json")
            .maxOutputTokens(geminiProperties.maxOutputTokens)
            .build()
    }

    private suspend fun acquireRateLimiterPermission(limiter: io.github.resilience4j.ratelimiter.RateLimiter) {
        withContext(ioDispatcher) {
            limiter.acquirePermission()
        }
    }

    companion object {
        private val MODEL = GeminiModelType.GEMINI_2_5_FLASH_LITE
    }
}
