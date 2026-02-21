package com.techinsights.batch.summary.service

import com.techinsights.batch.summary.config.props.BatchProcessingProperties
import com.techinsights.domain.enums.ErrorType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BatchRetryPolicyTest : FunSpec({

    val properties = BatchProcessingProperties(
        maxRetryAttempts = 3,
        retry = BatchProcessingProperties.RetryConfig(
            baseDelayMs = 100,
            rateLimitBaseDelayMs = 500,
            otherErrorBaseDelayMs = 200
        )
    )
    val policy = BatchRetryPolicy(properties)

    test("shouldRetry should return true for retryable errors within attempt limit") {
        policy.shouldRetry(ErrorType.TIMEOUT, 0) shouldBe true
        policy.shouldRetry(ErrorType.TIMEOUT, 2) shouldBe true
    }

    test("shouldRetry should return false when attempts exceeded") {
        policy.shouldRetry(ErrorType.TIMEOUT, 3) shouldBe false
    }

    test("shouldRetry should return false for non-retryable errors") {
        policy.shouldRetry(ErrorType.API_ERROR, 0) shouldBe false
    }

    test("calculateBackoffDelay should return correct delay for RateLimit") {
        policy.calculateBackoffDelay(ErrorType.RATE_LIMIT, 1) shouldBe 1000
    }

    test("calculateBackoffDelay should return correct delay for Timeout") {
        policy.calculateBackoffDelay(ErrorType.TIMEOUT, 0) shouldBe 200
    }
})
