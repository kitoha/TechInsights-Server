package com.techinsights.domain.dto.embedding

import com.techinsights.domain.entity.post.PostEmbedding
import com.techinsights.domain.repository.post.PostEmbeddingWithDistance
import com.techinsights.domain.utils.Tsid

data class PostEmbeddingDto(
    val postId: String,
    val companyName: String,
    val categories: String,
    val content: String,
    val embeddingVector: FloatArray,
    /** pgvector cosine distance (<->). Null when not retrieved from a similarity query. */
    val distance: Double? = null,
) {
    companion object {
        fun fromEntity(entity: PostEmbedding): PostEmbeddingDto =
            PostEmbeddingDto(
                postId = Tsid.encode(entity.postId),
                companyName = entity.companyName,
                categories = entity.categories,
                content = entity.content,
                embeddingVector = entity.embeddingVector,
            )

        fun fromProjection(projection: PostEmbeddingWithDistance): PostEmbeddingDto =
            PostEmbeddingDto(
                postId = Tsid.encode(projection.postId),
                companyName = projection.companyName,
                categories = projection.categories,
                content = projection.content,
                embeddingVector = FloatArray(0),   // not needed in semantic search pipeline
                distance = projection.distance,
            )
    }
}
