package com.techinsights.batch.github.embedding.processor

import com.techinsights.batch.github.embedding.dto.GithubEmbeddingRequestDto
import com.techinsights.domain.dto.embedding.EmbeddingRequest
import com.techinsights.domain.dto.github.GithubRepositoryDto
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class GithubReadmeEmbeddingProcessor : ItemProcessor<GithubRepositoryDto, GithubEmbeddingRequestDto> {

    override fun process(item: GithubRepositoryDto): GithubEmbeddingRequestDto? {
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

        return GithubEmbeddingRequestDto(
            id = item.id,
            fullName = item.fullName,
            promptString = request.toPromptString(),
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
