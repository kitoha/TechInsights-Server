package com.techinsights.domain.service.gemini

import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class BatchSummaryValidatorTest : FunSpec({

  val validator = BatchSummaryValidator()
  val validCategories = setOf("AI", "Backend", "Frontend", "DevOps", "Data")

  test("should validate successful result with all valid fields") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "1",
      success = true,
      summary = "This is a valid summary with more than 50 characters for testing purposes.",
      categories = listOf("AI", "Backend"),
      preview = "Valid preview"
    )

    val validation = validator.validate(input, result, validCategories)

    validation.isValid shouldBe true
    validation.errors.shouldBeEmpty()
  }

  test("should fail validation when ID mismatch") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "2",
      success = true,
      summary = "Valid summary with sufficient length for testing validation.",
      categories = listOf("AI"),
      preview = "Preview"
    )

    val validation = validator.validate(input, result, validCategories)

    validation.isValid shouldBe false
    validation.errors shouldContain "ID mismatch: expected 1, got 2"
  }

  test("should fail validation when result is marked as failed") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "1",
      success = false,
      summary = null,
      categories = null,
      preview = null,
      error = "API error occurred"
    )

    val validation = validator.validate(input, result, validCategories)

    validation.isValid shouldBe false
    validation.errors shouldContain "Result marked as failed: API error occurred"
  }

  test("should fail validation when summary is blank") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "1",
      success = true,
      summary = "",
      categories = listOf("AI"),
      preview = "Preview"
    )

    val validation = validator.validate(input, result, validCategories)

    validation.isValid shouldBe false
    validation.errors shouldContain "Summary is blank"
  }

  test("should fail validation when summary is null") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "1",
      success = true,
      summary = null,
      categories = listOf("AI"),
      preview = "Preview"
    )

    val validation = validator.validate(input, result, validCategories)

    validation.isValid shouldBe false
    validation.errors shouldContain "Summary is blank"
  }

  test("should fail validation when summary is too short") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "1",
      success = true,
      summary = "Short",
      categories = listOf("AI"),
      preview = "Preview"
    )

    val validation = validator.validate(input, result, validCategories)

    validation.isValid shouldBe false
    validation.errors shouldContain "Summary too short: 5 chars"
  }

  test("should fail validation when summary is too long") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "1",
      success = true,
      summary = "a".repeat(5001),
      categories = listOf("AI"),
      preview = "Preview"
    )

    val validation = validator.validate(input, result, validCategories)

    validation.isValid shouldBe false
    validation.errors shouldContain "Summary too long: 5001 chars"
  }

  test("should accept summary with exactly 50 characters") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "1",
      success = true,
      summary = "a".repeat(50),
      categories = listOf("AI"),
      preview = "Preview"
    )

    val validation = validator.validate(input, result, validCategories)

    validation.isValid shouldBe true
  }

  test("should accept summary with exactly 5000 characters") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "1",
      success = true,
      summary = "a".repeat(5000),
      categories = listOf("AI"),
      preview = "Preview"
    )

    val validation = validator.validate(input, result, validCategories)

    validation.isValid shouldBe true
  }

  test("should fail validation when categories is null") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "1",
      success = true,
      summary = "Valid summary with more than fifty characters for testing.",
      categories = null,
      preview = "Preview"
    )

    val validation = validator.validate(input, result, validCategories)

    validation.isValid shouldBe false
    validation.errors shouldContain "No categories provided"
  }

  test("should fail validation when categories is empty") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "1",
      success = true,
      summary = "Valid summary with more than fifty characters for testing.",
      categories = emptyList(),
      preview = "Preview"
    )

    val validation = validator.validate(input, result, validCategories)

    validation.isValid shouldBe false
    validation.errors shouldContain "No categories provided"
  }

  test("should fail validation when categories contain invalid values") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "1",
      success = true,
      summary = "Valid summary with more than fifty characters for testing.",
      categories = listOf("AI", "InvalidCategory", "AnotherInvalid"),
      preview = "Preview"
    )

    val validation = validator.validate(input, result, validCategories)

    validation.isValid shouldBe false
    validation.errors shouldHaveSize 1
    validation.errors.first() shouldContain "Invalid categories"
    validation.errors.first() shouldContain "InvalidCategory"
    validation.errors.first() shouldContain "AnotherInvalid"
  }

  test("should fail validation when preview is blank") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "1",
      success = true,
      summary = "Valid summary with more than fifty characters for testing.",
      categories = listOf("AI"),
      preview = ""
    )

    val validation = validator.validate(input, result, validCategories)

    validation.isValid shouldBe false
    validation.errors shouldContain "Preview is blank"
  }

  test("should fail validation when preview is null") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "1",
      success = true,
      summary = "Valid summary with more than fifty characters for testing.",
      categories = listOf("AI"),
      preview = null
    )

    val validation = validator.validate(input, result, validCategories)

    validation.isValid shouldBe false
    validation.errors shouldContain "Preview is blank"
  }

  test("should collect multiple validation errors") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "2",
      success = true,
      summary = "Short",
      categories = listOf("Invalid"),
      preview = ""
    )

    val validation = validator.validate(input, result, validCategories)

    validation.isValid shouldBe false
    validation.errors.size shouldBe 4
    validation.errors.any { it.contains("ID mismatch: expected 1, got 2") } shouldBe true
    validation.errors.any { it.contains("Summary too short: 5 chars") } shouldBe true
    validation.errors.any { it.contains("Invalid categories") } shouldBe true
    validation.errors.any { it.contains("Preview is blank") } shouldBe true
  }

  test("should validate with multiple valid categories") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "1",
      success = true,
      summary = "Valid summary with more than fifty characters for testing purposes.",
      categories = listOf("AI", "Backend", "Frontend"),
      preview = "Valid preview"
    )

    val validation = validator.validate(input, result, validCategories)

    validation.isValid shouldBe true
  }

  test("should fail when success is false but has error message") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "1",
      success = false,
      error = "Timeout occurred"
    )

    val validation = validator.validate(input, result, validCategories)

    validation.isValid shouldBe false
    validation.errors shouldHaveSize 1
    validation.errors.any { it.contains("Result marked as failed: Timeout occurred") } shouldBe true
  }

  test("should stop validation early when result is marked as failed") {
    val input = ArticleInput("1", "Title", "Content")
    val result = SummaryResultWithId(
      id = "1",
      success = false,
      summary = null,
      categories = null,
      preview = null,
      error = "Failed"
    )

    val validation = validator.validate(input, result, validCategories)

    validation.errors shouldHaveSize 1
  }
})
