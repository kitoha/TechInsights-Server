package com.techinsights.domain.dto.github

import com.techinsights.domain.entity.github.GithubRepository
import java.time.LocalDateTime

data class GithubRepositoryDto(
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
    val pushedAt: LocalDateTime,
    val fetchedAt: LocalDateTime,
    val weeklyStarDelta: Long,
    val readmeSummary: String?,
    val readmeSummarizedAt: LocalDateTime? = null,
) {
    companion object {
        fun fromEntity(entity: GithubRepository): GithubRepositoryDto {
            return GithubRepositoryDto(
                id = entity.id,
                repoName = entity.repoName,
                fullName = entity.fullName,
                description = entity.description,
                htmlUrl = entity.htmlUrl,
                starCount = entity.starCount,
                forkCount = entity.forkCount,
                primaryLanguage = entity.primaryLanguage,
                ownerName = entity.ownerName,
                ownerAvatarUrl = entity.ownerAvatarUrl,
                topics = entity.topics
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?: emptyList(),
                pushedAt = entity.pushedAt,
                fetchedAt = entity.fetchedAt,
                weeklyStarDelta = entity.weeklyStarDelta,
                readmeSummary = entity.readmeSummary,
                readmeSummarizedAt = entity.readmeSummarizedAt,
            )
        }
    }
}
