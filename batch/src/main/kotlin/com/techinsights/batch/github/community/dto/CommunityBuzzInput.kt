package com.techinsights.batch.github.community.dto

import com.techinsights.domain.dto.community.CommunityPost

data class CommunityBuzzInput(
    val repoFullName: String,
    val ownerName: String,
    val repoName: String,
    val hnPosts: List<CommunityPost>,
    val redditPosts: List<CommunityPost>,
    val prevMentionCount: Int?,
    val updateCount: Int,
) {
    val totalMentions: Int get() = hnPosts.size + redditPosts.size

    fun hasNewMentions(): Boolean =
        prevMentionCount == null || totalMentions > prevMentionCount
}
