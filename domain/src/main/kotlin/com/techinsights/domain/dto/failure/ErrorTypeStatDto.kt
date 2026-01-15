package com.techinsights.domain.dto.failure

import com.techinsights.domain.enums.SummaryErrorType

data class ErrorTypeStatDto(
    val errorType: SummaryErrorType,
    val count: Long
)
