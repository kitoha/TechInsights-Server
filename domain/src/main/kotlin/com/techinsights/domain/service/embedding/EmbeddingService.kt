package com.techinsights.domain.service.embedding

import com.techinsights.domain.dto.embedding.EmbeddingRequest
import com.techinsights.domain.enums.GeminiModelType

interface EmbeddingService {
    fun generateEmbedding(request: EmbeddingRequest, modelType: GeminiModelType): List<Float>

    fun generateEmbeddingBatch(requests: List<EmbeddingRequest>, modelType: GeminiModelType): List<EmbeddingResult>

    fun generateQuestionEmbedding(question: String): List<Float>
    
    data class EmbeddingResult(
        val request: EmbeddingRequest,
        val vector: List<Float>,
        val success: Boolean = true,
        val error: String? = null
    )
}
