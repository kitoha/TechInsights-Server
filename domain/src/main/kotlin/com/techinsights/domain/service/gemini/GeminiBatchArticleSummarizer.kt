package com.techinsights.domain.service.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.genai.Client
import com.google.genai.types.GenerateContentConfig
import com.techinsights.domain.config.gemini.GeminiProperties
import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import com.techinsights.domain.enums.Category
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
class GeminiBatchArticleSummarizer(
    private val geminiClient: Client,
    private val geminiProperties: GeminiProperties,
    private val promptBuilder: BatchPromptBuilder,
    rateLimiterRegistry: RateLimiterRegistry,
    circuitBreakerRegistry: CircuitBreakerRegistry,
    private val ioDispatcher: CoroutineContext = Dispatchers.IO
) : BatchArticleSummarizer {

    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = jacksonObjectMapper()

    private val rpmLimiter = rateLimiterRegistry.rateLimiter("geminiBatchRpm")
    private val rpdLimiter = rateLimiterRegistry.rateLimiter("geminiBatchRpd")
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker("geminiBatch")

    override fun summarizeBatch(
        articles: List<ArticleInput>,
        modelType: GeminiModelType
    ): Flow<SummaryResultWithId> {
        return channelFlow {
            val results = mutableSetOf<String>()
            var lastErrorType: ErrorType? = null
            var lastErrorMessage: String? = null
            
            callGeminiApi(articles, modelType).collect { result ->
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
        articles: List<ArticleInput>,
        modelType: GeminiModelType
    ): Flow<SummaryResultWithId> = channelFlow {
        val modelName = modelType.get()
        val categories = Category.entries.map { it.name }

        val prompt = promptBuilder.buildPrompt(articles, categories)
        val config = buildGeminiConfig(categories)

        acquireRateLimiterPermission(rpdLimiter)
        acquireRateLimiterPermission(rpmLimiter)

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
                    log.warn("Gemini stream interrupted. Reason: $finishReason, Articles: ${articles.map { it.id }}")
                    // 스트림이 중단되었으므로 에러 정보를 담아 보냄 (이후 정합성 체크에서 나머지 게시글에 적용됨)
                    send(SummaryResultWithId(
                        id = "SYSTEM_ERROR_MARKER", // 정합성 체크를 위한 마커
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
    }.buffer(Channel.UNLIMITED) // downstream 처리가 늦어져도 막히지 않도록 버퍼 설정

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
}
