package com.techinsights.api.exception

import java.time.LocalDateTime

data class ErrorResponse(
    val errorCode: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val path: String? = null,
    val details: Map<String, Any>? = null
)
