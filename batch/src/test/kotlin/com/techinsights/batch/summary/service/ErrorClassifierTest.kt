package com.techinsights.batch.summary.service

import com.techinsights.domain.enums.ErrorType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.TimeoutCancellationException

class ErrorClassifierTest : FunSpec({

    val classifier = ErrorClassifier()

    test("classify should return TIMEOUT for TimeoutCancellationException") {
        val exception = io.mockk.mockk<TimeoutCancellationException>()
        classifier.classify(exception) shouldBe ErrorType.TIMEOUT
    }

    test("classify should return RATE_LIMIT for 429 error") {
        classifier.classify(RuntimeException("429 Too Many Requests")) shouldBe ErrorType.RATE_LIMIT
    }

    test("classify should return TIMEOUT for timeout message") {
        classifier.classify(RuntimeException("Connection timeout")) shouldBe ErrorType.TIMEOUT
    }

    test("classify should return API_ERROR for unknown exception") {
        classifier.classify(RuntimeException("Something went wrong")) shouldBe ErrorType.API_ERROR
    }
})
