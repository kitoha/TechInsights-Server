package com.techinsights.batch.writer

import com.techinsights.domain.dto.embedding.PostEmbeddingDto
import com.techinsights.domain.entity.post.PostEmbedding
import com.techinsights.domain.repository.post.PostEmbeddingRepository
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class PostEmbeddingWriter(
    private val postEmbeddingRepository: PostEmbeddingRepository
) : ItemWriter<List<PostEmbeddingDto>> {

    override fun write(chunk: Chunk<out List<PostEmbeddingDto>>) {
        val dtos = chunk.items.flatten()
        if (dtos.isNotEmpty()) {
            val embeddings = dtos.map { dto ->
                PostEmbedding(
                    postId = dto.postId,
                    companyName = dto.companyName,
                    categories = dto.categories,
                    content = dto.content,
                    embeddingVector = dto.embeddingVector
                )
            }
            postEmbeddingRepository.saveAll(embeddings)
        }
    }
}
