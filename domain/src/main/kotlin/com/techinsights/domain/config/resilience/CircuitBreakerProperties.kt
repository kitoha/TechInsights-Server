package com.techinsights.domain.config.resilience

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "circuit-breaker")
data class CircuitBreakerProperties(
    val geminiBatch: CircuitBreakerConfig = CircuitBreakerConfig(),
    val communityInsight: CircuitBreakerConfig = CircuitBreakerConfig(
        slowCallDurationThresholdSeconds = 30,
        waitDurationInOpenStateSeconds = 120,
    ),
) {
    data class CircuitBreakerConfig(
        val failureRateThreshold: Int = 50,
        val slowCallRateThreshold: Int = 50,
        val slowCallDurationThresholdSeconds: Long = 60,
        val waitDurationInOpenStateSeconds: Long = 60,
        val permittedNumberOfCallsInHalfOpenState: Int = 3,
        val slidingWindowSize: Int = 10,
        val minimumNumberOfCalls: Int = 5
    )
}
