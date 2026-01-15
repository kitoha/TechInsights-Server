package com.techinsights.batch.mapper

import com.techinsights.batch.mapper.ErrorTypeMapper.toSummaryErrorType
import com.techinsights.domain.enums.ErrorType
import com.techinsights.domain.enums.SummaryErrorType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ErrorTypeMapperTest : FunSpec({

    test("should map API_ERROR to API_ERROR") {
        ErrorType.API_ERROR.toSummaryErrorType() shouldBe SummaryErrorType.API_ERROR
    }

    test("should map TIMEOUT to TIMEOUT") {
        ErrorType.TIMEOUT.toSummaryErrorType() shouldBe SummaryErrorType.TIMEOUT
    }
    
    test("should map RATE_LIMIT to API_ERROR") {
        ErrorType.RATE_LIMIT.toSummaryErrorType() shouldBe SummaryErrorType.API_ERROR
    }

    test("should map VALIDATION_ERROR to VALIDATION_ERROR") {
        ErrorType.VALIDATION_ERROR.toSummaryErrorType() shouldBe SummaryErrorType.VALIDATION_ERROR
    }
    
    test("should map CONTENT_ERROR to CONTENT_ERROR") {
        ErrorType.CONTENT_ERROR.toSummaryErrorType() shouldBe SummaryErrorType.CONTENT_ERROR
    }

    test("should map UNKNOWN to UNKNOWN") {
        ErrorType.UNKNOWN.toSummaryErrorType() shouldBe SummaryErrorType.UNKNOWN
    }
})
