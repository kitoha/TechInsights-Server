package com.techinsights.domain.dto.community

data class CommunityAnalysisInput(
    val repoFullName: String,
    val repoName: String,
    val hnPosts: List<CommunityPost>,
    val redditPosts: List<CommunityPost>,
)
