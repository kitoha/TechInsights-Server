package com.techinsights.api.github

import com.techinsights.domain.enums.CommunityStatus

data class GithubCommunityResponse(
    val communityStatus: CommunityStatus?,
    val mentionCount: Int?,
    val sentimentPositive: Int?,
    val sentimentNeutral: Int?,
    val sentimentNegative: Int?,
    val communityInsights: String?,
    val topPosts: List<CommunityPostResponse>?,
) {
    data class CommunityPostResponse(
        val platform: String,
        val title: String,
        val url: String,
        val score: Int,
        val commentCount: Int,
        val sentiment: String?,
    )
}
