package com.techinsights.domain.config.resilience

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

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
        registeredNames shouldContain "geminiBatchRpm"
    }

    test("일일 쿼터는 DB로 관리하므로 RPD RateLimiter는 등록되지 않는다") {
        val registeredNames = config.rateLimiterRegistry().allRateLimiters.map { it.name }

        registeredNames shouldNotContain "geminiBatchRpd"
        registeredNames shouldNotContain "geminiReadmeRpd"
        registeredNames shouldNotContain "geminiCommunityRpd"
    }
})
