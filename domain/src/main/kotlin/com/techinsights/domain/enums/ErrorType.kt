package com.techinsights.domain.enums

enum class ErrorType {
    API_ERROR,
    TIMEOUT,
    RATE_LIMIT,
    VALIDATION_ERROR,
    UNKNOWN,
    CONTENT_ERROR,
    SAFETY_BLOCKED,
    LENGTH_LIMIT;

    /** 인프라 오류(토큰 부족, API 장애 등)는 재시도 가능. 콘텐츠/정책 오류는 영구 실패. */
    val isRetryable: Boolean get() = this in RETRYABLE_TYPES

    companion object {
        val RETRYABLE_TYPES: Set<ErrorType> = setOf(LENGTH_LIMIT, API_ERROR, TIMEOUT, UNKNOWN, RATE_LIMIT)
    }
}
