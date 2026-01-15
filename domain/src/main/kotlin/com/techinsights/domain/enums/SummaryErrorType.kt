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
    SAFETY_BLOCKED(
        retryable = false,
        description = "Content blocked by safety filters"
    ),
    LENGTH_LIMIT(
        retryable = false,
        description = "Content or response length exceeded limits"
    ),
    UNKNOWN(
        retryable = true,
        description = "Unknown error occurred"
    );
}
