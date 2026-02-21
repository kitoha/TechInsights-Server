package com.techinsights.api.search

import com.techinsights.domain.dto.search.SemanticSearchResult
import com.techinsights.domain.dto.search.PostSearchResultDto
import com.techinsights.domain.dto.post.PostDto

data class SemanticSearchResponse(
    val query: String,
    val results: List<SemanticPostResult>,
    val totalReturned: Int,
    val processingTimeMs: Long
) {
    companion object {
        fun of(
            query: String,
            results: List<SemanticSearchResult>,
            processingTimeMs: Long
        ): SemanticSearchResponse {
            return SemanticSearchResponse(
                query = query,
                results = results.map { it.toApiResult() },
                totalReturned = results.size,
                processingTimeMs = processingTimeMs
            )
        }

        private fun SemanticSearchResult.toApiResult(): SemanticPostResult {
            return SemanticPostResult(
                post = post.toPostSearchResultDto(),
                similarityScore = similarityScore,
                rank = rank
            )
        }

        private fun PostDto.toPostSearchResultDto(): PostSearchResultDto {
            return PostSearchResultDto(
                id = id,
                title = title,
                preview = preview,
                url = url,
                thumbnail = thumbnail,
                companyId = company.id,
                companyName = company.name,
                companyLogo = company.logoImageName,
                viewCount = viewCount,
                publishedAt = publishedAt,
                isSummary = isSummary,
                categories = categories,
                relevanceScore = 0.0
            )
        }
    }
}
