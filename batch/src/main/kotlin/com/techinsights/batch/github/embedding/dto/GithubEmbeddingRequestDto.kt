package com.techinsights.batch.github.embedding.dto

data class GithubEmbeddingRequestDto(
    val id: Long,
    val fullName: String,
    val promptString: String,
)
