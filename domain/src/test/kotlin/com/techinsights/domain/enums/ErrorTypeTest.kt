package com.techinsights.domain.enums

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class ErrorTypeTest : FunSpec({

  test("should have all expected error types") {
    val errorTypes = ErrorType.values()

    errorTypes shouldContain ErrorType.API_ERROR
    errorTypes shouldContain ErrorType.TIMEOUT
    errorTypes shouldContain ErrorType.RATE_LIMIT
    errorTypes shouldContain ErrorType.VALIDATION_ERROR
    errorTypes shouldContain ErrorType.CONTENT_ERROR
    errorTypes shouldContain ErrorType.UNKNOWN
  }

  test("should have exactly 6 error types") {
    val errorTypes = ErrorType.values()

    errorTypes.size shouldBe 6
  }

  test("should convert from string correctly") {
    ErrorType.valueOf("API_ERROR") shouldBe ErrorType.API_ERROR
    ErrorType.valueOf("TIMEOUT") shouldBe ErrorType.TIMEOUT
    ErrorType.valueOf("RATE_LIMIT") shouldBe ErrorType.RATE_LIMIT
    ErrorType.valueOf("VALIDATION_ERROR") shouldBe ErrorType.VALIDATION_ERROR
    ErrorType.valueOf("CONTENT_ERROR") shouldBe ErrorType.CONTENT_ERROR
    ErrorType.valueOf("UNKNOWN") shouldBe ErrorType.UNKNOWN
  }

  test("should maintain order") {
    val errorTypes = ErrorType.values()

    errorTypes[0] shouldBe ErrorType.API_ERROR
    errorTypes[1] shouldBe ErrorType.TIMEOUT
    errorTypes[2] shouldBe ErrorType.RATE_LIMIT
    errorTypes[3] shouldBe ErrorType.VALIDATION_ERROR
    errorTypes[4] shouldBe ErrorType.UNKNOWN
    errorTypes[5] shouldBe ErrorType.CONTENT_ERROR
  }
})
