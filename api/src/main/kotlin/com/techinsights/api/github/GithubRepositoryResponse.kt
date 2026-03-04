package com.techinsights.api.github

import com.techinsights.domain.dto.github.GithubRepositoryDto
import java.time.LocalDateTime

data class GithubRepositoryResponse(
    val id: Long,
    val repoName: String,
    val fullName: String,
    val description: String?,
    val htmlUrl: String,
    val starCount: Long,
    val forkCount: Long,
    val primaryLanguage: String?,
    val ownerName: String,
    val ownerAvatarUrl: String?,
    val topics: List<String>,
    val weeklyStarDelta: Long,
    val pushedAt: LocalDateTime,
    val fetchedAt: LocalDateTime,
    val readmeSummary: String?,
    val readmeSummarizedAt: LocalDateTime?,
) {
    companion object {
        fun fromDto(dto: GithubRepositoryDto): GithubRepositoryResponse = GithubRepositoryResponse(
            id = dto.id,
            repoName = dto.repoName,
            fullName = dto.fullName,
            description = dto.description,
            htmlUrl = dto.htmlUrl,
            starCount = dto.starCount,
            forkCount = dto.forkCount,
            primaryLanguage = dto.primaryLanguage,
            ownerName = dto.ownerName,
            ownerAvatarUrl = dto.ownerAvatarUrl,
            topics = dto.topics,
            weeklyStarDelta = dto.weeklyStarDelta,
            pushedAt = dto.pushedAt,
            fetchedAt = dto.fetchedAt,
            readmeSummary = dto.readmeSummary,
            readmeSummarizedAt = dto.readmeSummarizedAt,
        )
    }
}
