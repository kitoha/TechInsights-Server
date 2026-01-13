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
class BatchPostEmbeddingWriter(
    private val postEmbeddingJpaRepository: PostEmbeddingJpaRepository,
    private val postRepository: PostRepository
) : ItemWriter<List<PostEmbeddingDto>> {

    private val log = LoggerFactory.getLogger(BatchPostEmbeddingWriter::class.java)

    override fun write(chunk: Chunk<out List<PostEmbeddingDto>>) {
        val allEmbeddings = chunk.items.flatten()

        if (allEmbeddings.isEmpty()) {
            log.warn("No embeddings to save in this chunk")
            return
        }

        log.info("Saving ${allEmbeddings.size} embeddings from ${chunk.items.size} batches")

        try {
            saveEmbeddings(allEmbeddings)
            updatePostEmbeddingStatus(allEmbeddings)

            log.info("Successfully saved ${allEmbeddings.size} embeddings and updated post statuses")
        } catch (e: Exception) {
            log.error("Failed to save embeddings", e)
            throw e
        }
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
        log.debug("Saved ${embeddings.size} embedding entities")
    }

    private fun updatePostEmbeddingStatus(items: List<PostEmbeddingDto>) {
        val postIds = items.map { it.postId }
        val updatedCount = postRepository.updateEmbeddingStatusBulk(postIds)

        if (updatedCount != postIds.size.toLong()) {
            log.warn(
                "Embedding status update mismatch: expected ${postIds.size}, but updated $updatedCount posts"
            )
        } else {
            log.debug("Updated embedding status for $updatedCount posts")
        }
    }
}
