package com.techinsights.domain.service.embedding

import com.google.genai.Client
import com.google.genai.types.EmbedContentConfig
import com.techinsights.domain.dto.embedding.EmbeddingRequest
import com.techinsights.domain.enums.GeminiModelType
import org.springframework.stereotype.Service

@Service
class EmbeddingServiceImpl(
  private val geminiClient: Client
) : EmbeddingService {

  override fun generateEmbedding(request: EmbeddingRequest, modelType: GeminiModelType): List<Float> {
    val modelName = modelType.get()
    val combinedInput = request.toPromptString()

    val embedContentConfig: EmbedContentConfig = EmbedContentConfig.builder()
      .taskType("SEMANTIC_SIMILARITY")
      .build()
    val response = geminiClient.models.embedContent(modelName, combinedInput, embedContentConfig)
    val embeddings = response.embeddings().orElse(emptyList())
    val first = embeddings.firstOrNull() ?: return emptyList()
    return first.values().orElse(emptyList())
  }

  override fun generateEmbeddingBatch(
    requests: List<EmbeddingRequest>,
    modelType: GeminiModelType
  ): List<EmbeddingService.EmbeddingResult> {
    if (requests.isEmpty()) return emptyList()
    
    val modelName = modelType.get()
    val embedContentConfig: EmbedContentConfig = EmbedContentConfig.builder()
      .taskType("SEMANTIC_SIMILARITY")
      .build()

    return requests.map { request ->
      try {
        val combinedInput = request.toPromptString()
        val response = geminiClient.models.embedContent(modelName, combinedInput, embedContentConfig)
        val embeddings = response.embeddings().orElse(emptyList())
        val first = embeddings.firstOrNull()
        
        if (first != null) {
          EmbeddingService.EmbeddingResult(
            request = request,
            vector = first.values().orElse(emptyList()),
            success = true
          )
        } else {
          EmbeddingService.EmbeddingResult(
            request = request,
            vector = emptyList(),
            success = false,
            error = "No embedding returned"
          )
        }
      } catch (e: Exception) {
        EmbeddingService.EmbeddingResult(
          request = request,
          vector = emptyList(),
          success = false,
          error = e.message ?: "Unknown error"
        )
      }
    }
  }
}