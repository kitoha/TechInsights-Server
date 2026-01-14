package com.techinsights.domain.exception

class JsonTruncationException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
