package com.techinsights.domain.enums


enum class SummaryErrorType(
    val retryable: Boolean,
    val description: String
) {
    API_ERROR(
        retryable = true,
        description = "API service error or unavailability"
    ),
    CONTENT_ERROR(
        retryable = false,
        description = "Content validation or processing error"
    ),
    JSON_TRUNCATION(
        retryable = true,
        description = "Response truncated or failed to parse"
    ),
    VALIDATION_ERROR(
        retryable = true,
        description = "Response validation failed"
    ),
    CIRCUIT_OPEN(
        retryable = true,
        description = "Circuit breaker open, service unavailable"
    ),
    TIMEOUT(
        retryable = true,
        description = "Request timed out"
    ),
    UNKNOWN(
        retryable = true,
        description = "Unknown error occurred"
    );

    companion object {
        fun fromException(exception: Exception): SummaryErrorType {
            val message = exception.message?.lowercase() ?: ""

            return when {
                message.contains("503") ||
                message.contains("overloaded") ||
                message.contains("service unavailable") -> API_ERROR

                message.contains("429") ||
                message.contains("rate limit") -> API_ERROR

                message.contains("timeout") ||
                message.contains("timed out") -> TIMEOUT

                message.contains("json") ||
                message.contains("parse") ||
                message.contains("truncat") ||
                message.contains("unexpected end") -> JSON_TRUNCATION

                message.contains("circuit") ||
                message.contains("breaker") -> CIRCUIT_OPEN

                message.contains("invalid content") ||
                message.contains("inappropriate") -> CONTENT_ERROR

                message.contains("validation") -> VALIDATION_ERROR

                else -> UNKNOWN
            }
        }

        fun nonRetryableTypes(): List<SummaryErrorType> {
            return entries.filter { !it.retryable }
        }
    }
}
