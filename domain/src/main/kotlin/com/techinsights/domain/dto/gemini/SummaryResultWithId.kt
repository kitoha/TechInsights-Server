package com.techinsights.domain.dto.gemini

data class SummaryResultWithId(
    val id: String,
    val success: Boolean,
    val summary: String? = null,
    val categories: List<String>? = null,
    val preview: String? = null,
    val error: String? = null
)
