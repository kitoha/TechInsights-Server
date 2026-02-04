package com.techinsights.batch.service

import com.techinsights.batch.dto.BatchFailure
import com.techinsights.batch.dto.BatchMetrics
import com.techinsights.batch.dto.BatchRequest
import com.techinsights.batch.dto.BatchResult
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.ErrorType
import org.springframework.stereotype.Component

@Component
class BatchResultAssembler {
    
    fun assembleResult(
        request: BatchRequest,
        successes: List<PostDto>,
        failures: List<BatchFailure>,
        duration: Long
    ): BatchResult {
        return BatchResult(
            requestId = request.id,
            successes = successes,
            failures = failures,
            metrics = createMetrics(request, successes.size, failures.size, duration)
        )
    }
    
    fun assembleFailureResult(
        request: BatchRequest,
        reason: String,
        errorType: ErrorType,
        retryable: Boolean
    ): BatchResult {
        val failures = request.posts.map { post ->
            BatchFailure(
                post = post,
                reason = reason,
                retryable = retryable,
                errorType = errorType
            )
        }
        
        return BatchResult(
            requestId = request.id,
            successes = emptyList(),
            failures = failures,
            metrics = createEmptyMetrics(request)
        )
    }
    
    private fun createMetrics(
        request: BatchRequest,
        successCount: Int,
        failureCount: Int,
        duration: Long
    ): BatchMetrics {
        return BatchMetrics(
            totalItems = request.posts.size,
            successCount = successCount,
            failureCount = failureCount,
            apiCallCount = 1,
            tokensUsed = request.estimatedTokens,
            durationMs = duration
        )
    }
    
    private fun createEmptyMetrics(request: BatchRequest): BatchMetrics {
        return BatchMetrics(
            totalItems = request.posts.size,
            successCount = 0,
            failureCount = request.posts.size,
            apiCallCount = 0,
            tokensUsed = 0,
            durationMs = 0
        )
    }
}
