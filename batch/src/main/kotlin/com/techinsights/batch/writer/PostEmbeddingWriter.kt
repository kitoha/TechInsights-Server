package com.techinsights.batch.writer

import com.techinsights.domain.dto.embedding.PostEmbeddingDto
import com.techinsights.domain.entity.post.PostEmbedding
import com.techinsights.domain.repository.post.PostEmbeddingJpaRepository
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.utils.Tsid
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class PostEmbeddingWriter(
    private val postEmbeddingJpaRepository: PostEmbeddingJpaRepository,
    private val postRepository: PostRepository
) : ItemWriter<PostEmbeddingDto?> {

    private val log = LoggerFactory.getLogger(PostEmbeddingWriter::class.java)

    override fun write(chunk: Chunk<out PostEmbeddingDto>) {
        val validItems = chunk.items.filterNotNull()
        if (validItems.isEmpty()) {
            return
        }

        saveEmbeddings(validItems)

        updatePostEmbeddingStatus(validItems)

        log.info("Successfully saved ${validItems.size} embeddings and updated post statuses")
    }

    private fun saveEmbeddings(items: List<PostEmbeddingDto>) {
        val embeddings = items.map { dto ->
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

    private fun updatePostEmbeddingStatus(items: List<PostEmbeddingDto>) {
        val postIds = items.map { it.postId }
        val updatedCount = postRepository.updateEmbeddingStatusBulk(postIds)

        if (updatedCount != postIds.size.toLong()) {
            log.warn(
                "Embedding status update mismatch: expected ${postIds.size}, but updated $updatedCount posts"
            )
        }
    }
}
