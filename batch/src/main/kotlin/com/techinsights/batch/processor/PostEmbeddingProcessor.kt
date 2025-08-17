package com.techinsights.batch.processor

import com.techinsights.domain.dto.embedding.EmbeddingRequest
import com.techinsights.domain.dto.embedding.PostEmbeddingDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.repository.post.PostEmbeddingRepository
import com.techinsights.domain.service.embedding.EmbeddingService
import com.techinsights.domain.utils.Tsid
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class PostEmbeddingProcessor(
    private val embeddingService: EmbeddingService,
    private val postEmbeddingRepository: PostEmbeddingRepository
) : ItemProcessor<List<PostDto>, List<PostEmbeddingDto>> {

    override fun process(items: List<PostDto>): List<PostEmbeddingDto> {
        val postIds = items.map { Tsid.decode(it.id) }.toSet()
        val existingEmbeddings = postEmbeddingRepository.findAllById(postIds).map { it.postId }.toSet()

        val newEmbeddingDtos = mutableListOf<PostEmbeddingDto>()

        items.forEach { post ->
            val postId = Tsid.decode(post.id)
            // 요약이 완료되었고, 미리보기가 있으며, 아직 임베딩이 없는 경우에만 처리합니다.
            if (post.isSummary && !post.preview.isNullOrBlank() && !existingEmbeddings.contains(postId)) {

                val request = EmbeddingRequest(
                    content = post.preview!!,
                    categories = post.categories.map { it.name },
                    companyName = post.company.name
                )

                val vector = embeddingService.generateEmbedding(request, GeminiModelType.GEMINI_EMBEDDING)

                val postEmbeddingDto = PostEmbeddingDto(
                    postId = postId,
                    companyName = post.company.name,
                    categories = post.categories.joinToString(",") { it.name },
                    content = post.preview!!,
                    embeddingVector = vector.toFloatArray()
                )
                newEmbeddingDtos.add(postEmbeddingDto)
            }
        }
        return newEmbeddingDtos
    }
}
