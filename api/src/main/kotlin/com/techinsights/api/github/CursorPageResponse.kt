package com.techinsights.api.github

data class CursorPageResponse<T>(
    val content: List<T>,
    val size: Int,
    val hasNext: Boolean,
    val nextCursor: String?,
)
