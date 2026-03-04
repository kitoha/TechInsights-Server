package com.techinsights.batch.github.embedding.processor

import com.techinsights.batch.github.embedding.dto.GithubEmbeddingResultDto
import com.techinsights.domain.dto.embedding.EmbeddingRequest
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.service.embedding.EmbeddingService
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class GithubReadmeEmbeddingProcessor(
    private val embeddingService: EmbeddingService,
) : ItemProcessor<GithubRepositoryDto, GithubEmbeddingResultDto> {

    override fun process(item: GithubRepositoryDto): GithubEmbeddingResultDto? {
        val content = buildContent(item)
        if (content.isBlank()) {
            log.warn("[EmbeddingProcessor] No content to embed for ${item.fullName}, skipping")
            return null
        }

        val request = EmbeddingRequest(
            content = content,
            categories = item.topics,
            companyName = item.ownerName,
        )

        val results = embeddingService.generateEmbeddingBatch(
            listOf(request),
            GeminiModelType.GEMINI_EMBEDDING,
        )

        val result = results.firstOrNull()
        if (result == null || !result.success || result.vector.isEmpty()) {
            log.warn("[EmbeddingProcessor] Failed to embed ${item.fullName}: ${result?.error}")
            return null
        }

        return GithubEmbeddingResultDto(
            id = item.id,
            fullName = item.fullName,
            embeddingVector = result.vector.toFloatArray(),
        )
    }

    private fun buildContent(item: GithubRepositoryDto): String {
        return listOfNotNull(
            item.description?.takeIf { it.isNotBlank() },
            item.readmeSummary?.takeIf { it.isNotBlank() },
        ).joinToString("\n\n")
    }

    companion object {
        private val log = LoggerFactory.getLogger(GithubReadmeEmbeddingProcessor::class.java)
    }
}
