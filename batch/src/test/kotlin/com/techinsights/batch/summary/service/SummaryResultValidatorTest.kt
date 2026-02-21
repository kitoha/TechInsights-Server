package com.techinsights.batch.summary.service

import com.techinsights.domain.dto.gemini.SummaryResultWithId
import com.techinsights.domain.service.gemini.BatchSummaryValidator
import com.techinsights.domain.service.gemini.BatchSummaryValidator.ValidationResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class SummaryResultValidatorTest : FunSpec({

    val batchValidator = mockk<BatchSummaryValidator>()
    val validator = SummaryResultValidator(batchValidator)

    test("validate should delegate to batchValidator and return valid result") {
        val result = SummaryResultWithId("1", true, "S", emptyList(), "P", null)
        
        every { batchValidator.validate(any(), any(), any()) } returns ValidationResult(true, emptyList())

        val validation = validator.validate("1", "T", "C", result)

        validation.isValid shouldBe true
        validation.errors.isEmpty() shouldBe true
    }

    test("validate should delegate to batchValidator and return invalid result") {
        val result = SummaryResultWithId("1", true, "S", emptyList(), "P", null)
        
        every { batchValidator.validate(any(), any(), any()) } returns ValidationResult(false, listOf("Error"))

        val validation = validator.validate("1", "T", "C", result)

        validation.isValid shouldBe false
        validation.errors shouldBe listOf("Error")
    }
})
