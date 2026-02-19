package com.techinsights.batch.summary.dto

data class BatchMetrics(
    val totalItems: Int,
    val successCount: Int,
    val failureCount: Int,
    val apiCallCount: Int,
    val tokensUsed: Int,
    val durationMs: Long
)
