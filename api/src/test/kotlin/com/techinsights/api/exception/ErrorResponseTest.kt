package com.techinsights.api.exception

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDateTime

class ErrorResponseTest : FunSpec({

    test("ErrorResponse should be created with all parameters") {
        // given
        val errorCode = "TEST_ERROR"
        val message = "Test error message"
        val timestamp = LocalDateTime.now()
        val path = "/api/test"
        val details = mapOf("field" to "value")

        // when
        val errorResponse = ErrorResponse(
            errorCode = errorCode,
            message = message,
            timestamp = timestamp,
            path = path,
            details = details
        )

        // then
        errorResponse.errorCode shouldBe errorCode
        errorResponse.message shouldBe message
        errorResponse.timestamp shouldBe timestamp
        errorResponse.path shouldBe path
        errorResponse.details shouldBe details
    }

    test("ErrorResponse should be created with minimal parameters (only errorCode and message)") {
        // given
        val errorCode = "MINIMAL_ERROR"
        val message = "Minimal error"

        // when
        val errorResponse = ErrorResponse(
            errorCode = errorCode,
            message = message
        )

        // then
        errorResponse.errorCode shouldBe errorCode
        errorResponse.message shouldBe message
        errorResponse.timestamp shouldNotBe null
        (errorResponse.timestamp is LocalDateTime) shouldBe true
        errorResponse.path shouldBe null
        errorResponse.details shouldBe null
    }

    test("ErrorResponse should auto-generate timestamp when not provided") {
        // given
        val beforeCreation = LocalDateTime.now()

        // when
        val errorResponse = ErrorResponse(
            errorCode = "AUTO_TIMESTAMP",
            message = "Auto timestamp test"
        )

        val afterCreation = LocalDateTime.now()

        // then
        errorResponse.timestamp shouldNotBe null
        errorResponse.timestamp.isAfter(beforeCreation.minusSeconds(1)) shouldBe true
        errorResponse.timestamp.isBefore(afterCreation.plusSeconds(1)) shouldBe true
    }

    test("ErrorResponse should handle null path") {
        // when
        val errorResponse = ErrorResponse(
            errorCode = "NULL_PATH",
            message = "Null path test",
            path = null
        )

        // then
        errorResponse.path shouldBe null
    }

    test("ErrorResponse should handle null details") {
        // when
        val errorResponse = ErrorResponse(
            errorCode = "NULL_DETAILS",
            message = "Null details test",
            details = null
        )

        // then
        errorResponse.details shouldBe null
    }

    test("ErrorResponse should handle empty details map") {
        // when
        val errorResponse = ErrorResponse(
            errorCode = "EMPTY_DETAILS",
            message = "Empty details test",
            details = emptyMap()
        )

        // then
        errorResponse.details shouldNotBe null
        errorResponse.details?.isEmpty() shouldBe true
    }

    test("ErrorResponse should handle complex details map") {
        // given
        val complexDetails = mapOf(
            "stringField" to "value",
            "numberField" to 123,
            "booleanField" to true,
            "listField" to listOf("a", "b", "c"),
            "nestedMap" to mapOf("key" to "value")
        )

        // when
        val errorResponse = ErrorResponse(
            errorCode = "COMPLEX_DETAILS",
            message = "Complex details test",
            details = complexDetails
        )

        // then
        errorResponse.details shouldBe complexDetails
        errorResponse.details?.size shouldBe 5
        errorResponse.details?.get("stringField") shouldBe "value"
        errorResponse.details?.get("numberField") shouldBe 123
        errorResponse.details?.get("booleanField") shouldBe true
    }

    test("ErrorResponse data class should support copy") {
        // given
        val original = ErrorResponse(
            errorCode = "ORIGINAL",
            message = "Original message"
        )

        // when
        val copied = original.copy(message = "Modified message")

        // then
        copied.errorCode shouldBe "ORIGINAL"
        copied.message shouldBe "Modified message"
        copied.timestamp shouldBe original.timestamp
    }

    test("ErrorResponse data class should support equality") {
        // given
        val timestamp = LocalDateTime.now()
        val error1 = ErrorResponse(
            errorCode = "ERROR",
            message = "Message",
            timestamp = timestamp,
            path = "/api/test"
        )
        val error2 = ErrorResponse(
            errorCode = "ERROR",
            message = "Message",
            timestamp = timestamp,
            path = "/api/test"
        )

        // then
        error1 shouldBe error2
    }

    test("ErrorResponse should handle long error messages") {
        // given
        val longMessage = "A".repeat(1000)

        // when
        val errorResponse = ErrorResponse(
            errorCode = "LONG_MESSAGE",
            message = longMessage
        )

        // then
        errorResponse.message shouldBe longMessage
        errorResponse.message.length shouldBe 1000
    }

    test("ErrorResponse should handle special characters in errorCode") {
        // given
        val specialErrorCode = "ERROR_CODE_WITH_UNDERSCORES_AND_NUMBERS_123"

        // when
        val errorResponse = ErrorResponse(
            errorCode = specialErrorCode,
            message = "Test"
        )

        // then
        errorResponse.errorCode shouldBe specialErrorCode
    }

    test("ErrorResponse should handle Korean characters in message") {
        // given
        val koreanMessage = "한글 에러 메시지입니다"

        // when
        val errorResponse = ErrorResponse(
            errorCode = "KOREAN_MESSAGE",
            message = koreanMessage
        )

        // then
        errorResponse.message shouldBe koreanMessage
    }
})
