package com.techinsights.domain.dto.community

data class CommunityPost(
    val platform: String,
    val title: String,
    val score: Int,
    val commentCount: Int,
    val username: String?,
    val url: String,
)
