package com.techinsights.domain.dto.github

import com.techinsights.domain.entity.github.GithubRepository
import com.techinsights.domain.enums.CommunityStatus
import com.techinsights.domain.enums.ErrorType
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
    val dailyStarDelta: Long,
    val readmeSummary: String?,
    val readmeSummarizedAt: LocalDateTime? = null,
    val readmeEmbeddedAt: LocalDateTime? = null,
    val communityCollectedAt: LocalDateTime? = null,
    val communityRawMentionCount: Int? = null,
    val communityHighlights: String? = null,
    val communityFetchedAt: LocalDateTime? = null,
    val communityMentionCount: Int? = null,
    val communityStatus: CommunityStatus? = null,
    val communityErrorType: ErrorType? = null,
    val communityUpdateCount: Int = 0,
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
                dailyStarDelta = entity.dailyStarDelta,
                readmeSummary = entity.readmeSummary,
                readmeSummarizedAt = entity.readmeSummarizedAt,
                readmeEmbeddedAt = entity.readmeEmbeddedAt,
                communityCollectedAt = entity.communityCollectedAt,
                communityRawMentionCount = entity.communityRawMentionCount,
                communityHighlights = entity.communityHighlights,
                communityFetchedAt = entity.communityFetchedAt,
                communityMentionCount = entity.communityMentionCount,
                communityStatus = entity.communityStatus,
                communityErrorType = entity.communityErrorType,
                communityUpdateCount = entity.communityUpdateCount,
            )
        }
    }
}
