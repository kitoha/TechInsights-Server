package com.techinsights.batch.config.props

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "web-client")
data class WebClientProperties(
    val connectTimeoutMillis: Int = 10000,
    val responseTimeoutSeconds: Long = 15,
    val readTimeoutSeconds: Long = 15,
    val writeTimeoutSeconds: Long = 15,
    val maxInMemorySizeMb: Int = 5,
    val userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    val retry: RetryConfig = RetryConfig()
) {
    val maxInMemorySizeBytes: Int
        get() = maxInMemorySizeMb * 1024 * 1024

    data class RetryConfig(
        val maxAttempts: Long = 3,
        val backoffSeconds: Long = 2
    )
}
