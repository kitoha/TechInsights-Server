package com.techinsights.batch.mapper

import com.techinsights.domain.enums.ErrorType
import com.techinsights.domain.enums.SummaryErrorType

object ErrorTypeMapper {

    fun ErrorType.toSummaryErrorType(): SummaryErrorType {
        return when (this) {
            ErrorType.API_ERROR -> SummaryErrorType.API_ERROR
            ErrorType.TIMEOUT -> SummaryErrorType.TIMEOUT
            ErrorType.RATE_LIMIT -> SummaryErrorType.API_ERROR
            ErrorType.VALIDATION_ERROR -> SummaryErrorType.VALIDATION_ERROR
            ErrorType.CONTENT_ERROR -> SummaryErrorType.CONTENT_ERROR
            ErrorType.SAFETY_BLOCKED -> SummaryErrorType.SAFETY_BLOCKED
            ErrorType.LENGTH_LIMIT -> SummaryErrorType.LENGTH_LIMIT
            else -> SummaryErrorType.UNKNOWN
        }
    }
}
