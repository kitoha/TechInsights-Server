package com.techinsights.batch.summary.dto

import com.techinsights.domain.dto.post.PostDto

data class BatchResult(
    val requestId: String,
    val successes: List<PostDto>,
    val failures: List<BatchFailure>,
    val metrics: BatchMetrics
)
