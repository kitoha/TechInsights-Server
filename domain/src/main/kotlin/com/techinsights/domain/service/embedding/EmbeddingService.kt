package com.techinsights.domain.service.embedding

import com.techinsights.domain.dto.embedding.EmbeddingRequest
import com.techinsights.domain.enums.GeminiModelType

interface EmbeddingService {
    fun generateEmbedding(request: EmbeddingRequest, modelType: GeminiModelType): List<Float>
}