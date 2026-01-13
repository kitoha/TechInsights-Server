package com.techinsights.domain.service.embedding

import com.google.genai.Client
import com.google.genai.types.EmbedContentConfig
import com.techinsights.domain.dto.embedding.EmbeddingRequest
import com.techinsights.domain.enums.GeminiModelType
import org.springframework.stereotype.Service

@Service
class EmbeddingServiceImpl(
  private val geminiClient: Client,
  rateLimiterRegistry: io.github.resilience4j.ratelimiter.RateLimiterRegistry
) : EmbeddingService {

  private val rateLimiter = rateLimiterRegistry.rateLimiter("geminiEmbedding")

  override fun generateEmbedding(request: EmbeddingRequest, modelType: GeminiModelType): List<Float> {
    return rateLimiter.executeSupplier {
      val modelName = modelType.get()
      val combinedInput = request.toPromptString()

      val embedContentConfig = buildEmbedConfig()
      val response = geminiClient.models.embedContent(modelName, combinedInput, embedContentConfig)
      
      extractEmbeddingVector(response)
    }
  }

  override fun generateEmbeddingBatch(
    requests: List<EmbeddingRequest>,
    modelType: GeminiModelType
  ): List<EmbeddingService.EmbeddingResult> {
    if (requests.isEmpty()) return emptyList()
    
    val modelName = modelType.get()
    val embedContentConfig = buildEmbedConfig()

    return requests.map { request ->
      generateSingleEmbeddingWithRateLimit(request, modelName, embedContentConfig)
    }
  }

  private fun generateSingleEmbeddingWithRateLimit(
    request: EmbeddingRequest,
    modelName: String,
    config: EmbedContentConfig
  ): EmbeddingService.EmbeddingResult {
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
}