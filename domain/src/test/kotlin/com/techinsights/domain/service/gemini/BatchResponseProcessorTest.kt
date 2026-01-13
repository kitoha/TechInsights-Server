package com.techinsights.domain.service.gemini

import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.BatchSummaryResponse
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*

class BatchResponseProcessorTest : FunSpec({

    lateinit var validator: BatchSummaryValidator
    lateinit var processor: BatchResponseProcessor

    beforeEach {
        validator = mockk()
        processor = BatchResponseProcessor(validator)
    }

    afterEach {
        clearAllMocks()
    }

    test("process should return valid results when all validations pass") {
        // given
        val articles = listOf(
            ArticleInput("1", "Title 1", "Content 1"),
            ArticleInput("2", "Title 2", "Content 2")
        )

        val response = BatchSummaryResponse(
            listOf(
                SummaryResultWithId(id = "1", success = true, summary = "Summary 1", categories = listOf("AI"), preview = "Preview 1", error = null),
                SummaryResultWithId(id = "2", success = true, summary = "Summary 2", categories = listOf("Backend"), preview = "Preview 2", error = null)
            )
        )

        val categories = setOf("AI", "Backend", "DevOps")

        every { validator.validate(any(), any(), categories) } returns BatchSummaryValidator.ValidationResult(true, emptyList())

        // when
        val result = processor.process(response, articles, categories)

        // then
        result.results shouldHaveSize 2
        result.results.all { it.success } shouldBe true

        verify(exactly = 2) { validator.validate(any(), any(), categories) }
    }

    test("process should mark result as failed when validation fails") {
        // given
        val articles = listOf(ArticleInput("1", "Title", "Content"))
        val response = BatchSummaryResponse(
            listOf(SummaryResultWithId(id = "1", success = true, summary = "Summary", categories = listOf("AI"), preview = "Preview", error = null))
        )
        val categories = setOf("AI")

        every { validator.validate(any(), any(), categories) } returns BatchSummaryValidator.ValidationResult(
            false,
            listOf("Invalid summary format")
        )

        // when
        val result = processor.process(response, articles, categories)

        // then
        result.results shouldHaveSize 1
        result.results[0].success shouldBe false
        result.results[0].error shouldBe "Invalid summary format"
    }

    test("process should create missing results for articles without responses") {
        // given
        val articles = listOf(
            ArticleInput("1", "Title 1", "Content 1"),
            ArticleInput("2", "Title 2", "Content 2"),
            ArticleInput("3", "Title 3", "Content 3")
        )

        val response = BatchSummaryResponse(
            listOf(
                SummaryResultWithId(id = "1", success = true, summary = "Summary 1", categories = listOf("AI"), preview = "Preview 1", error = null)
            )
        )

        val categories = setOf("AI")

        every { validator.validate(any(), any(), categories) } returns BatchSummaryValidator.ValidationResult(true, emptyList())

        // when
        val result = processor.process(response, articles, categories)

        // then
        result.results shouldHaveSize 3
        result.results.count { it.success } shouldBe 1
        result.results.count { !it.success } shouldBe 2

        val missingResults = result.results.filter { it.id in setOf("2", "3") }
        missingResults.all { it.success == false } shouldBe true
        missingResults.all { it.error == "No response received" } shouldBe true
    }

    test("process should mark result as failed for unknown article ID") {
        // given
        val articles = listOf(ArticleInput("1", "Title", "Content"))
        val response = BatchSummaryResponse(
            listOf(
                SummaryResultWithId(id = "1", success = true, summary = "Summary 1", categories = listOf("AI"), preview = "Preview 1", error = null),
                SummaryResultWithId(id = "999", success = true, summary = "Summary 999", categories = listOf("Backend"), preview = "Preview 999", error = null)
            )
        )
        val categories = setOf("AI", "Backend")

        every { validator.validate(any(), any(), categories) } returns BatchSummaryValidator.ValidationResult(true, emptyList())

        // when
        val result = processor.process(response, articles, categories)

        // then
        result.results shouldHaveSize 2

        val unknownResult = result.results.find { it.id == "999" }
        unknownResult?.success shouldBe false
        unknownResult?.error shouldBe "Unknown ID"
    }

    test("process should handle response count mismatch") {
        // given
        val articles = listOf(
            ArticleInput("1", "Title 1", "Content 1"),
            ArticleInput("2", "Title 2", "Content 2")
        )

        val response = BatchSummaryResponse(
            listOf(
                SummaryResultWithId(id = "1", success = true, summary = "Summary", categories = listOf("AI"), preview = "Preview", error = null)
            )
        )

        val categories = setOf("AI")

        every { validator.validate(any(), any(), categories) } returns BatchSummaryValidator.ValidationResult(true, emptyList())

        // when
        val result = processor.process(response, articles, categories)

        // then
        result.results shouldHaveSize 2
        result.results[0].id shouldBe "1"
        result.results[0].success shouldBe true
        result.results[1].id shouldBe "2"
        result.results[1].success shouldBe false
        result.results[1].error shouldBe "No response received"
    }

    test("process should validate each result with corresponding article") {
        // given
        val articles = listOf(
            ArticleInput("1", "Title 1", "Content 1"),
            ArticleInput("2", "Title 2", "Content 2")
        )

        val response = BatchSummaryResponse(
            listOf(
                SummaryResultWithId(id = "1", success = true, summary = "Summary 1", categories = listOf("AI"), preview = "Preview 1", error = null),
                SummaryResultWithId(id = "2", success = true, summary = "Summary 2", categories = listOf("Backend"), preview = "Preview 2", error = null)
            )
        )

        val categories = setOf("AI", "Backend")

        every { validator.validate(any(), any(), categories) } returns BatchSummaryValidator.ValidationResult(true, emptyList())

        // when
        processor.process(response, articles, categories)

        // then
        verify(exactly = 1) {
            validator.validate(
                match { it.id == "1" && it.title == "Title 1" },
                match { it.id == "1" && it.summary == "Summary 1" },
                categories
            )
        }
        verify(exactly = 1) {
            validator.validate(
                match { it.id == "2" && it.title == "Title 2" },
                match { it.id == "2" && it.summary == "Summary 2" },
                categories
            )
        }
    }

    test("process should preserve original result data when validation passes") {
        // given
        val articles = listOf(ArticleInput("1", "Title", "Content"))
        val originalResult = SummaryResultWithId(
            id = "1",
            success = true,
            summary = "Original Summary",
            preview = "Original Preview",
            categories = listOf("AI", "Backend"),
            error = null
        )
        val response = BatchSummaryResponse(listOf(originalResult))
        val categories = setOf("AI", "Backend")

        every { validator.validate(any(), any(), categories) } returns BatchSummaryValidator.ValidationResult(true, emptyList())

        // when
        val result = processor.process(response, articles, categories)

        // then
        result.results shouldHaveSize 1
        result.results[0].id shouldBe "1"
        result.results[0].success shouldBe true
        result.results[0].summary shouldBe "Original Summary"
        result.results[0].preview shouldBe "Original Preview"
        result.results[0].categories shouldBe listOf("AI", "Backend")
    }

    test("process should handle empty response and create missing results for all articles") {
        // given
        val articles = listOf(
            ArticleInput("1", "Title 1", "Content 1"),
            ArticleInput("2", "Title 2", "Content 2")
        )
        val response = BatchSummaryResponse(emptyList())
        val categories = setOf("AI")

        // when
        val result = processor.process(response, articles, categories)

        // then
        result.results shouldHaveSize 2
        result.results.all { !it.success } shouldBe true
        result.results.all { it.error == "No response received" } shouldBe true
        result.results.map { it.id } shouldContainAll listOf("1", "2")
    }

    test("process should handle validation errors with multiple error messages") {
        // given
        val articles = listOf(ArticleInput("1", "Title", "Content"))
        val response = BatchSummaryResponse(
            listOf(SummaryResultWithId(id = "1", success = true, summary = "Summary", categories = listOf("AI"), preview = "Preview", error = null))
        )
        val categories = setOf("AI")

        every { validator.validate(any(), any(), categories) } returns BatchSummaryValidator.ValidationResult(
            false,
            listOf("Error 1", "Error 2", "Error 3")
        )

        // when
        val result = processor.process(response, articles, categories)

        // then
        result.results shouldHaveSize 1
        result.results[0].success shouldBe false
        result.results[0].error shouldBe "Error 1, Error 2, Error 3"
    }

    test("process should handle mix of valid and invalid results") {
        // given
        val articles = listOf(
            ArticleInput("1", "Title 1", "Content 1"),
            ArticleInput("2", "Title 2", "Content 2"),
            ArticleInput("3", "Title 3", "Content 3")
        )

        val response = BatchSummaryResponse(
            listOf(
                SummaryResultWithId(id = "1", success = true, summary = "Summary 1", categories = listOf("AI"), preview = "Preview 1", error = null),
                SummaryResultWithId(id = "2", success = true, summary = "Summary 2", categories = listOf("Backend"), preview = "Preview 2", error = null),
                SummaryResultWithId(id = "3", success = true, summary = "Summary 3", categories = listOf("DevOps"), preview = "Preview 3", error = null)
            )
        )

        val categories = setOf("AI", "Backend", "DevOps")

        every { validator.validate(match { it.id == "1" }, any(), categories) } returns BatchSummaryValidator.ValidationResult(true, emptyList())
        every { validator.validate(match { it.id == "2" }, any(), categories) } returns BatchSummaryValidator.ValidationResult(false, listOf("Invalid"))
        every { validator.validate(match { it.id == "3" }, any(), categories) } returns BatchSummaryValidator.ValidationResult(true, emptyList())

        // when
        val result = processor.process(response, articles, categories)

        // then
        result.results shouldHaveSize 3
        result.results[0].success shouldBe true
        result.results[1].success shouldBe false
        result.results[2].success shouldBe true
    }
})
