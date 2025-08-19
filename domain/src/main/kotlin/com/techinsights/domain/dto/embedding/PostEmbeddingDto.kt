package com.techinsights.domain.dto.embedding

import com.techinsights.domain.entity.post.PostEmbedding

data class PostEmbeddingDto(
    val postId: Long,
    val companyName: String,
    val categories: String,
    val content: String,
    val embeddingVector: FloatArray
){
    companion object{
        fun fromEntity(entity: PostEmbedding): PostEmbeddingDto {
            return PostEmbeddingDto(
                postId = entity.postId,
                companyName = entity.companyName,
                categories = entity.categories,
                content = entity.content,
                embeddingVector = entity.embeddingVector
            )
        }
    }
}
