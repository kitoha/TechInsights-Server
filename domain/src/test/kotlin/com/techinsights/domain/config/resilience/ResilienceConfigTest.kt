package com.techinsights.domain.config.resilience

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain

class ResilienceConfigTest : FunSpec({

    val config = ResilienceConfig(
        rateLimiterProperties = RateLimiterProperties(),
        circuitBreakerProperties = CircuitBreakerProperties()
    )

    test("모든 RateLimiter가 레지스트리에 등록된다") {
        val registry = config.rateLimiterRegistry()
        val registeredNames = registry.allRateLimiters.map { it.name }

        registeredNames shouldContain "githubApi"
        registeredNames shouldContain "geminiReadmeRpm"
        registeredNames shouldContain "geminiReadmeRpd"
        registeredNames shouldContain "geminiBatchRpm"
        registeredNames shouldContain "geminiBatchRpd"
    }
})
