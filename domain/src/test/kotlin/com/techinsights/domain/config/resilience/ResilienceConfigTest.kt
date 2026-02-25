package com.techinsights.domain.config.resilience

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull

class ResilienceConfigTest : FunSpec({

    val config = ResilienceConfig(
        rateLimiterProperties = RateLimiterProperties(),
        circuitBreakerProperties = CircuitBreakerProperties()
    )

    test("githubApi RateLimiter가 레지스트리에 등록된다") {
        val registry = config.rateLimiterRegistry()

        registry.rateLimiter("githubApi").shouldNotBeNull()
    }

    test("geminiReadmeRpm RateLimiter가 레지스트리에 등록된다") {
        val registry = config.rateLimiterRegistry()

        registry.rateLimiter("geminiReadmeRpm").shouldNotBeNull()
    }

    test("geminiReadmeRpd RateLimiter가 레지스트리에 등록된다") {
        val registry = config.rateLimiterRegistry()

        registry.rateLimiter("geminiReadmeRpd").shouldNotBeNull()
    }

    test("기존 geminiBatchRpm, geminiBatchRpd RateLimiter가 유지된다") {
        val registry = config.rateLimiterRegistry()

        registry.rateLimiter("geminiBatchRpm").shouldNotBeNull()
        registry.rateLimiter("geminiBatchRpd").shouldNotBeNull()
    }
})
