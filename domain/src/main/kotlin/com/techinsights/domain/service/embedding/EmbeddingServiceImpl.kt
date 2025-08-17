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
    val modelName = modelType.getModelName()
    val combinedInput = request.toPromptString()

    val embedContentConfig: EmbedContentConfig = EmbedContentConfig.builder()
      .taskType("SEMANTIC_SIMILARITY")
      .build()
    val response = geminiClient.models.embedContent(modelName, combinedInput, embedContentConfig)
    val embeddings = response.embeddings().orElse(emptyList())
    val first = embeddings.firstOrNull() ?: return emptyList()
    return first.values().orElse(emptyList())
  }
}