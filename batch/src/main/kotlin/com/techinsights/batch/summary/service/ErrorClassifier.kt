package com.techinsights.batch.summary.service

import com.techinsights.domain.enums.ErrorType
import kotlinx.coroutines.TimeoutCancellationException
import org.springframework.stereotype.Component

@Component
class ErrorClassifier {
    
    fun classify(exception: Exception): ErrorType {
        return when {
            exception is TimeoutCancellationException -> ErrorType.TIMEOUT
            isRateLimitError(exception) -> ErrorType.RATE_LIMIT
            isTimeoutError(exception) -> ErrorType.TIMEOUT
            else -> ErrorType.API_ERROR
        }
    }
    
    private fun isRateLimitError(exception: Exception): Boolean {
        val message = exception.message?.lowercase() ?: return false
        return RATE_LIMIT_KEYWORDS.any { message.contains(it) }
    }
    
    private fun isTimeoutError(exception: Exception): Boolean {
        val message = exception.message?.lowercase() ?: return false
        return TIMEOUT_KEYWORDS.any { message.contains(it) }
    }
    
    companion object {
        private val RATE_LIMIT_KEYWORDS = setOf(
            "rate limit",
            "overloaded",
            "503",
            "429"
        )
        
        private val TIMEOUT_KEYWORDS = setOf(
            "timeout"
        )
    }
}
