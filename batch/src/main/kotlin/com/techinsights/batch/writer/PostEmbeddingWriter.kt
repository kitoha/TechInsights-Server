package com.techinsights.batch.writer

import com.techinsights.domain.dto.embedding.PostEmbeddingDto
import com.techinsights.domain.entity.post.PostEmbedding
import com.techinsights.domain.repository.post.PostEmbeddingJpaRepository
import com.techinsights.domain.utils.Tsid
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class PostEmbeddingWriter(
    private val postEmbeddingJpaRepository: PostEmbeddingJpaRepository
) : ItemWriter<PostEmbeddingDto?> {

    override fun write(chunk: Chunk<out PostEmbeddingDto>) {
        val items = chunk.items
        if (items.isNotEmpty()) {
            val filteredItems = items.filterNotNull()
            val embeddings = filteredItems.map { dto ->
                PostEmbedding(
                    postId = Tsid.decode(dto.postId),
                    companyName = dto.companyName,
                    categories = dto.categories,
                    content = dto.content,
                    embeddingVector = dto.embeddingVector
                )
            }
            postEmbeddingJpaRepository.saveAll(embeddings)
        }
    }
}
