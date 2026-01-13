package com.techinsights.batch.processor

import com.techinsights.domain.dto.embedding.EmbeddingRequest
import com.techinsights.domain.dto.embedding.PostEmbeddingDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.service.embedding.EmbeddingService
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class BatchPostEmbeddingProcessor(
    private val embeddingService: EmbeddingService,
) : ItemProcessor<List<PostDto>, List<PostEmbeddingDto>> {

    private val log = LoggerFactory.getLogger(BatchPostEmbeddingProcessor::class.java)

    override fun process(items: List<PostDto>): List<PostEmbeddingDto>? {
        if (items.isEmpty()) return null

        val validPosts = items.filter { isValidForEmbedding(it) }

        if (validPosts.isEmpty()) {
            log.warn("No valid posts for embedding in batch of ${items.size} items")
            return null
        }

        log.info("Processing batch of ${validPosts.size} posts for embedding (filtered from ${items.size})")

        try {
            val requests = validPosts.map { post ->
                EmbeddingRequest(
                    content = post.preview!!,
                    categories = post.categories.map { it.name },
                    companyName = post.company.name
                )
            }

            val results = embeddingService.generateEmbeddingBatch(
                requests,
                GeminiModelType.GEMINI_EMBEDDING
            )

            val embeddingDtos = mutableListOf<PostEmbeddingDto>()
            val failures = mutableListOf<String>()

            results.forEachIndexed { index, result ->
                val post = validPosts[index]

                if (result.success && result.vector.isNotEmpty()) {
                    embeddingDtos.add(
                        PostEmbeddingDto(
                            postId = post.id,
                            companyName = post.company.name,
                            categories = post.categories.joinToString(",") { it.name },
                            content = post.preview!!,
                            embeddingVector = result.vector.toFloatArray()
                        )
                    )
                } else {
                    failures.add("Post ${post.id}: ${result.error ?: "Unknown error"}")
                }
            }

            log.info(
                "Batch embedding complete: ${embeddingDtos.size} successes, ${failures.size} failures"
            )

            if (failures.isNotEmpty()) {
                log.warn("Failed embeddings: ${failures.take(5).joinToString("; ")}")
            }

            return if (embeddingDtos.isNotEmpty()) embeddingDtos else null

        } catch (e: Exception) {
            log.error("Batch embedding failed for ${validPosts.size} posts", e)
            throw e
        }
    }

    private fun isValidForEmbedding(post: PostDto): Boolean {
        if (!post.isSummary) {
            log.debug("Post ${post.id} is not summarized, skipping")
            return false
        }

        if (post.preview.isNullOrBlank()) {
            log.warn("Post ${post.id} has no preview content, skipping")
            return false
        }

        return true
    }
}
