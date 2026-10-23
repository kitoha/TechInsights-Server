package com.techinsights.domain.infrastructure.gemini.embedding

import com.techinsights.domain.service.embedding.EmbeddingService
import com.google.genai.Client
import com.google.genai.types.EmbedContentConfig
import com.techinsights.domain.config.cache.CacheConfig
import com.techinsights.domain.dto.embedding.EmbeddingRequest
import com.techinsights.domain.infrastructure.gemini.GeminiModelType
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import mu.KotlinLogging
import com.techinsights.domain.service.quota.DailyQuota
import com.techinsights.domain.service.quota.DailyQuotaService
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class EmbeddingServiceImpl(
  private val geminiClient: Client,
  rateLimiterRegistry: RateLimiterRegistry,
  cacheManager: CacheManager,
  private val dailyQuotaService: DailyQuotaService
) : EmbeddingService {

  private val rateLimiter = rateLimiterRegistry.rateLimiter("geminiEmbedding")
  private val queryRateLimiter = rateLimiterRegistry.rateLimiter("geminiQueryEmbedding")
  private val queryEmbeddingCache = requireNotNull(cacheManager.getCache(CacheConfig.QUERY_EMBEDDING)) {
    "Cache ${CacheConfig.QUERY_EMBEDDING} is not registered"
  }

  override fun generateEmbedding(request: EmbeddingRequest): List<Float> {
    if (!dailyQuotaService.tryReserve(DailyQuota.EMBEDDING)) {
      log.warn { "Daily Gemini embedding quota exhausted" }
      return emptyList()
    }
    return rateLimiter.executeSupplier {
      val modelName = MODEL.get()
      val combinedInput = request.toPromptString()

      val embedContentConfig = buildEmbedConfig()
      val response = geminiClient.models.embedContent(modelName, combinedInput, embedContentConfig)

      extractEmbeddingVector(response)
    }
  }

  override fun generateQuestionEmbedding(question: String): List<Float> {
    val normalizedQuestion = normalizeQuestion(question)
    val vector = try {
      queryEmbeddingCache.get(normalizedQuestion) { requestQuestionEmbedding(normalizedQuestion) }.orEmpty()
    } catch (e: Cache.ValueRetrievalException) {
      throw e.cause ?: e
    }
    if (vector.isEmpty()) queryEmbeddingCache.evict(normalizedQuestion)
    return vector
  }

  private fun requestQuestionEmbedding(question: String): List<Float> {
    val request = EmbeddingRequest(
      content = question,
      categories = emptyList(),
      companyName = ""
    )
    return queryRateLimiter.executeSupplier {
      val response = geminiClient.models.embedContent(
        MODEL.get(),
        request.toPromptString(),
        buildEmbedConfig()
      )
      extractEmbeddingVector(response)
    }
  }

  private fun normalizeQuestion(question: String): String =
    question.trim().replace(WHITESPACE, " ").lowercase()

  override fun generateEmbeddingBatch(
    requests: List<EmbeddingRequest>
  ): List<EmbeddingService.EmbeddingResult> {
    if (requests.isEmpty()) return emptyList()

    val modelName = MODEL.get()
    val embedContentConfig = buildEmbedConfig()

    return requests.map { request ->
      generateSingleEmbeddingWithRateLimit(request, modelName, embedContentConfig)
    }
  }

  override fun batchEmbedTexts(
    promptStrings: List<String>
  ): List<List<Float>> {
    if (promptStrings.isEmpty()) return emptyList()

    if (!dailyQuotaService.tryReserve(DailyQuota.EMBEDDING)) {
      log.warn { "Daily Gemini embedding quota exhausted. texts=${promptStrings.size}" }
      return promptStrings.map { emptyList() }
    }

    return rateLimiter.executeSupplier {
      val modelName = MODEL.get()
      val config = buildEmbedConfig()
      val response = geminiClient.models.embedContent(modelName, promptStrings, config)
      val embeddings = response.embeddings().orElse(emptyList())
      embeddings.map { it.values().orElse(emptyList()) }
    }
  }

  private fun generateSingleEmbeddingWithRateLimit(
    request: EmbeddingRequest,
    modelName: String,
    config: EmbedContentConfig
  ): EmbeddingService.EmbeddingResult {
    if (!dailyQuotaService.tryReserve(DailyQuota.EMBEDDING)) {
      return EmbeddingService.EmbeddingResult(
        request = request,
        vector = emptyList(),
        success = false,
        error = QUOTA_EXHAUSTED_ERROR
      )
    }
    return try {
      rateLimiter.executeSupplier {
        val combinedInput = request.toPromptString()
        val response = geminiClient.models.embedContent(modelName, combinedInput, config)
        val vector = extractEmbeddingVector(response)

        if (vector.isNotEmpty()) {
          EmbeddingService.EmbeddingResult(
            request = request,
            vector = vector,
            success = true
          )
        } else {
          EmbeddingService.EmbeddingResult(
            request = request,
            vector = emptyList(),
            success = false,
            error = "No embedding returned from API"
          )
        }
      }
    } catch (e: Exception) {
      EmbeddingService.EmbeddingResult(
        request = request,
        vector = emptyList(),
        success = false,
        error = e.message ?: "Unknown error occurred"
      )
    }
  }

  private fun buildEmbedConfig(): EmbedContentConfig {
    return EmbedContentConfig.builder()
      .taskType("SEMANTIC_SIMILARITY")
      .build()
  }

  private fun extractEmbeddingVector(response: com.google.genai.types.EmbedContentResponse): List<Float> {
    val embeddings = response.embeddings().orElse(emptyList())
    val first = embeddings.firstOrNull() ?: return emptyList()
    return first.values().orElse(emptyList())
  }

  companion object {
    private val MODEL = GeminiModelType.GEMINI_EMBEDDING
    private const val QUOTA_EXHAUSTED_ERROR = "Daily Gemini quota exhausted"
    private val log = KotlinLogging.logger {}
    private val WHITESPACE = Regex("\\s+")
  }
}
