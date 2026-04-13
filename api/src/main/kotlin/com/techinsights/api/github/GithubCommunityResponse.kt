package com.techinsights.api.github

import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.enums.CommunityStatus

data class GithubCommunityResponse(
    val communityStatus: CommunityStatus?,
    val communitySentiment: String?,
    val communityInsights: String?,
    val communityHighlights: String?,
    val communityMentionCount: Int?,
) {
    companion object {
        fun fromDto(dto: GithubRepositoryDto): GithubCommunityResponse = GithubCommunityResponse(
            communityStatus = dto.communityStatus,
            communitySentiment = dto.communitySentiment,
            communityInsights = dto.communityInsights,
            communityHighlights = dto.communityHighlights,
            communityMentionCount = dto.communityMentionCount,
        )
    }
}
