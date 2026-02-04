package com.techinsights.batch.dto

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.ErrorType

data class BatchFailure(
    val post: PostDto,
    val reason: String,
    val retryable: Boolean,
    val errorType: ErrorType,
    val batchSize: Int = 1,
    val isBatchFailure: Boolean = false
)
