package com.techinsights.batch.config.props

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "batch.processing")
data class BatchProcessingProperties(
    var concurrencyLimit: Int = 2,
    var timeoutMs: Long = 60_000,
    var maxRetryAttempts: Int = 2,
    var retry: RetryConfig = RetryConfig()
) {
    data class RetryConfig(
        var baseDelayMs: Long = 2000,
        var rateLimitBaseDelayMs: Long = 10000,
        var otherErrorBaseDelayMs: Long = 5000
    )
}
