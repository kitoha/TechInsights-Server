package com.techinsights.domain.config.resilience

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RateLimiterPropertiesTest : FunSpec({

    test("github 기본값 - 분당 25건, 30초 타임아웃") {
        val props = RateLimiterProperties()

        props.github.limitForPeriod shouldBe 25
        props.github.refreshPeriodSeconds shouldBe 60
        props.github.timeoutSeconds shouldBe 30
    }

    test("geminiReadmeRpm 기본값 - 분당 2건 (250K TPM 제약)") {
        val props = RateLimiterProperties()

        props.geminiReadmeRpm.limitForPeriod shouldBe 2
        props.geminiReadmeRpm.refreshPeriodSeconds shouldBe 60
        props.geminiReadmeRpm.timeoutSeconds shouldBe 60
    }
})
