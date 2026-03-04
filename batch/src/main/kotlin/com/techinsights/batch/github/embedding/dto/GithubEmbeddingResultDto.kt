package com.techinsights.batch.github.embedding.dto

data class GithubEmbeddingResultDto(
    val id: Long,
    val fullName: String,
    val embeddingVector: FloatArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GithubEmbeddingResultDto) return false
        return id == other.id && fullName == other.fullName && embeddingVector.contentEquals(other.embeddingVector)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + fullName.hashCode()
        result = 31 * result + embeddingVector.contentHashCode()
        return result
    }
}
