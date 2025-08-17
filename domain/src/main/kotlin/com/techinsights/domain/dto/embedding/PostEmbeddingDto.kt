package com.techinsights.domain.dto.embedding

data class PostEmbeddingDto(
    val postId: Long,
    val companyName: String,
    val categories: String,
    val content: String,
    val embeddingVector: FloatArray
)
