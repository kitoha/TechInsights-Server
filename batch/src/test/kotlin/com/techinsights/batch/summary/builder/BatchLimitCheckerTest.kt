package com.techinsights.batch.summary.builder

import com.techinsights.batch.summary.config.BatchBuildConfig
import com.techinsights.domain.config.gemini.GeminiProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class BatchLimitCheckerTest : FunSpec({

    val config = BatchBuildConfig(
        maxTokensPerRequest = 10000,
        maxBatchSize = 10,
        basePromptTokens = 100,
        avgTokensPerSummary = 500,
        jsonOverheadTokens = 200,
        outputSafetyMargin = 0.9,
        truncationBufferTokens = 1000,
        tokensPerChar = 4
    )
    val geminiProperties = mockk<GeminiProperties>()

    every { geminiProperties.maxOutputTokens } returns 8192

    val limitChecker = BatchLimitChecker(config, geminiProperties)

    test("exceedsInputLimit should return true when tokens exceed limit") {
        limitChecker.exceedsInputLimit(9000, 1001) shouldBe true
        limitChecker.exceedsInputLimit(9000, 1000) shouldBe false
    }

    test("exceedsOutputLimit should return true when estimated output exceeds limit") {

        limitChecker.exceedsOutputLimit(14) shouldBe false

        limitChecker.exceedsOutputLimit(15) shouldBe true
    }

    test("exceedsBatchSize should return true when size exceeds limit") {
        limitChecker.exceedsBatchSize(10) shouldBe true
        limitChecker.exceedsBatchSize(9) shouldBe false
    }

    test("exceedsMaxTokens should return true when single post exceeds limit") {
        limitChecker.exceedsMaxTokens(10001) shouldBe true
        limitChecker.exceedsMaxTokens(10000) shouldBe false
    }

    test("estimateOutputTokens should calculate correctly") {
        limitChecker.estimateOutputTokens(2) shouldBe 1200
    }
})
