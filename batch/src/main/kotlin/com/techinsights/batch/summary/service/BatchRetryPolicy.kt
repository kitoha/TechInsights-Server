package com.techinsights.batch.summary.service

import com.techinsights.batch.summary.config.props.BatchProcessingProperties
import com.techinsights.domain.enums.ErrorType
import org.springframework.stereotype.Component

@Component
class BatchRetryPolicy(
    private val properties: BatchProcessingProperties
) {
    
    fun shouldRetry(errorType: ErrorType, currentAttempt: Int): Boolean {
        return isRetryableError(errorType) && currentAttempt < properties.maxRetryAttempts
    }
    
    fun calculateBackoffDelay(errorType: ErrorType, retryCount: Int): Long {
        val baseDelay = when (errorType) {
            ErrorType.RATE_LIMIT -> properties.retry.rateLimitBaseDelayMs
            ErrorType.TIMEOUT -> properties.retry.otherErrorBaseDelayMs
            else -> properties.retry.baseDelayMs
        }
        return baseDelay * (retryCount + 1)
    }
    
    fun isRetryableError(errorType: ErrorType): Boolean {
        return errorType in RETRYABLE_ERROR_TYPES
    }
    
    companion object {
        private val RETRYABLE_ERROR_TYPES = setOf(
            ErrorType.TIMEOUT,
            ErrorType.RATE_LIMIT
        )
    }
}
