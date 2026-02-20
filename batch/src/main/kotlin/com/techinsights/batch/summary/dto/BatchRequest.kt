package com.techinsights.batch.summary.dto

import com.techinsights.domain.dto.post.PostDto

data class BatchRequest(
    val id: String,
    val posts: List<PostDto>,
    val estimatedTokens: Int,
    val priority: Int = 0
)
