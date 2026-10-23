package com.techinsights.domain.service.embedding

import com.techinsights.domain.dto.embedding.EmbeddingRequest

interface EmbeddingService {
    fun generateEmbedding(request: EmbeddingRequest): List<Float>

    fun generateEmbeddingBatch(requests: List<EmbeddingRequest>): List<EmbeddingResult>

    fun generateQuestionEmbedding(question: String): List<Float>

    fun batchEmbedTexts(promptStrings: List<String>): List<List<Float>>

    data class EmbeddingResult(
        val request: EmbeddingRequest,
        val vector: List<Float>,
        val success: Boolean = true,
        val error: String? = null
    )
}
