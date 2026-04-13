package com.techinsights.domain.dto.community

import java.time.LocalDateTime

data class CommunityPost(
    val platform: String,
    val title: String,
    val score: Int,
    val commentCount: Int,
    val username: String? = null,
    val url: String,
    val postCreatedAt: LocalDateTime? = null,
)
