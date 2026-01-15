package com.techinsights.domain.enums

enum class ErrorType {
    API_ERROR,
    TIMEOUT,
    RATE_LIMIT,
    VALIDATION_ERROR,
    UNKNOWN,
    CONTENT_ERROR,
    SAFETY_BLOCKED,
    LENGTH_LIMIT
}
