package com.techinsights.batch.github.community.service

import com.techinsights.batch.github.community.dto.CommunityBuzzInput

fun interface CommunityFetcher {
    suspend fun fetch(
        repoFullName: String,
        ownerName: String,
        repoName: String,
        prevMentionCount: Int?,
        updateCount: Int,
    ): CommunityBuzzInput
}
