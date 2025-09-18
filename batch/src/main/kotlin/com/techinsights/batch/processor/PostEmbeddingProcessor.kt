package com.techinsights.batch.processor

import com.techinsights.domain.dto.embedding.EmbeddingRequest
import com.techinsights.domain.dto.embedding.PostEmbeddingDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.repository.post.PostEmbeddingJpaRepository
import com.techinsights.domain.service.embedding.EmbeddingService
import com.techinsights.domain.utils.Tsid
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class PostEmbeddingProcessor(
    private val embeddingService: EmbeddingService,
) : ItemProcessor<PostDto, PostEmbeddingDto> {

    private val log = LoggerFactory.getLogger(PostEmbeddingProcessor::class.java)

    override fun process(item: PostDto): PostEmbeddingDto {
        val postId = item.let { Tsid.decode(it.id) }

        if (item.isSummary && !item.preview.isNullOrBlank()) {

            val request = EmbeddingRequest(
                content = item.preview!!,
                categories = item.categories.map { it.name },
                companyName = item.company.name
            )

            val vector =
                embeddingService.generateEmbedding(request, GeminiModelType.GEMINI_EMBEDDING)

            val postEmbeddingDto = PostEmbeddingDto(
                postId = item.id,
                companyName = item.company.name,
                categories = item.categories.joinToString(",") { it.name },
                content = item.preview!!,
                embeddingVector = vector.toFloatArray()
            )

            log.info("Successfully Vector Embedding item with id: ${item.id}")
            return postEmbeddingDto
        } else {
            log.warn("failed Vector Embedding item with id: ${item.id}")
            throw IllegalArgumentException("Post with id $postId is not summarized or preview is blank.")
        }
    }
}
