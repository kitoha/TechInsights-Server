package com.techinsights.api.github

import com.techinsights.domain.dto.github.GithubSemanticSearchResult

data class GithubSearchResultResponse(
    val fullName: String,
    val repoName: String,
    val description: String?,
    val readmeSummary: String?,
    val primaryLanguage: String?,
    val starCount: Long,
    val ownerName: String,
    val ownerAvatarUrl: String?,
    val topics: List<String>,
    val htmlUrl: String,
    val similarityScore: Double,
    val rank: Int,
) {

    companion object {
        fun fromDto(dto: GithubSemanticSearchResult): GithubSearchResultResponse = GithubSearchResultResponse(
            fullName = dto.fullName,
            repoName = dto.repoName,
            description = dto.description,
            readmeSummary = dto.readmeSummary,
            primaryLanguage = dto.primaryLanguage,
            starCount = dto.starCount,
            ownerName = dto.ownerName,
            ownerAvatarUrl = dto.ownerAvatarUrl,
            topics = dto.topics,
            htmlUrl = dto.htmlUrl,
            similarityScore = dto.similarityScore,
            rank = dto.rank,
        )
    }
}
