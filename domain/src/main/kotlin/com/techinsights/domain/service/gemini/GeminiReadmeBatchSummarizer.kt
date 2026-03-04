package com.techinsights.domain.service.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.genai.Client
import com.google.genai.types.GenerateContentConfig
import com.techinsights.domain.config.gemini.GeminiProperties
import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import com.techinsights.domain.enums.ErrorType
import com.techinsights.domain.enums.GeminiModelType
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.coroutines.CoroutineContext

@Service
class GeminiReadmeBatchSummarizer(
    private val geminiClient: Client,
    private val geminiProperties: GeminiProperties,
    private val promptBuilder: GithubReadmePromptBuilder,
    rateLimiterRegistry: RateLimiterRegistry,
    circuitBreakerRegistry: CircuitBreakerRegistry,
    private val ioDispatcher: CoroutineContext = Dispatchers.IO,
) : GithubReadmeBatchSummarizer {

    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = jacksonObjectMapper()

    private val rpmLimiter = rateLimiterRegistry.rateLimiter("geminiReadmeRpm")
    private val rpdLimiter = rateLimiterRegistry.rateLimiter("geminiReadmeRpd")
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker("geminiBatch")

    override fun summarize(items: List<ArticleInput>): Flow<SummaryResultWithId> {
        if (items.isEmpty()) return channelFlow { }

        return channelFlow {
            val responded = mutableSetOf<String>()
            var lastErrorType: ErrorType? = null
            var lastErrorMessage: String? = null

            callGeminiApi(items).collect { result ->
                if (result.id == SYSTEM_ERROR_MARKER) {
                    lastErrorType = result.errorType
                    lastErrorMessage = result.error
                } else {
                    responded.add(result.id)
                    if (!result.success) {
                        lastErrorType = result.errorType
                        lastErrorMessage = result.error
                    }
                    send(result)
                }
            }

            // 응답에서 누락된 ID → 실패로 처리
            items.filter { it.id !in responded }.forEach { missing ->
                val errorType = lastErrorType ?: ErrorType.CONTENT_ERROR
                val errorMessage = lastErrorMessage ?: "Response missing from Gemini stream"
                log.warn("README ${missing.id} missing from Gemini response. errorType=$errorType")
                send(SummaryResultWithId(id = missing.id, success = false, error = errorMessage, errorType = errorType))
            }
        }.buffer(Channel.UNLIMITED)
    }

    private fun callGeminiApi(items: List<ArticleInput>): Flow<SummaryResultWithId> = channelFlow {
        val modelName = GeminiModelType.GEMINI_2_5_FLASH.get()
        val prompt = promptBuilder.buildPrompt(items)
        val config = buildGeminiConfig()

        acquireRateLimiterPermission()

        try {
            val responseStream = circuitBreaker.executeCallable {
                geminiClient.models.generateContentStream(modelName, prompt, config)
            }

            val jsonParser = StreamingJsonParser()
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
                    log.warn("Gemini stream interrupted. reason=$finishReason, repos=${items.map { it.id }}")
                    send(SummaryResultWithId(
                        id = SYSTEM_ERROR_MARKER,
                        success = false,
                        error = "Gemini finished with reason: $finishReason",
                        errorType = currentErrorType,
                    ))
                }

                res.text()?.let { chunk ->
                    jsonParser.process(chunk).forEach { result ->
                        launch(ioDispatcher) { send(result) }
                    }
                }
            }
        } catch (e: Exception) {
            log.error("README batch summarization failed for [${items.map { it.id }.joinToString()}].", e)
            items.forEach { item ->
                launch(ioDispatcher) {
                    send(SummaryResultWithId(item.id, false, error = e.message ?: "Streaming failed", errorType = ErrorType.API_ERROR))
                }
            }
        }
    }.buffer(Channel.UNLIMITED)

    private fun buildGeminiConfig(): GenerateContentConfig {
        val schema = promptBuilder.buildSchema()
        val schemaNode = mapper.readTree(schema)

        return GenerateContentConfig.builder()
            .responseJsonSchema(schemaNode)
            .responseMimeType("application/json")
            .maxOutputTokens(geminiProperties.maxOutputTokens)
            .build()
    }

    private suspend fun acquireRateLimiterPermission() {
        withContext(ioDispatcher) {
            rpdLimiter.acquirePermission()
            rpmLimiter.acquirePermission()
        }
    }

    companion object {
        private const val SYSTEM_ERROR_MARKER = "SYSTEM_ERROR_MARKER"
    }
}
