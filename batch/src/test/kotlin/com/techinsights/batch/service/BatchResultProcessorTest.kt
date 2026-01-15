package com.techinsights.batch.service

import com.techinsights.batch.dto.BatchRequest
import com.techinsights.batch.dto.BatchResult
import com.techinsights.batch.service.SummaryResultValidator.ValidationResult
import com.techinsights.domain.dto.gemini.BatchSummaryResponse
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.ErrorType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

class BatchResultProcessorTest : FunSpec({

    val validator = mockk<SummaryResultValidator>()
    val converter = mockk<PostDtoConverter>()
    val assembler = mockk<BatchResultAssembler>()
    val retryPolicy = mockk<BatchRetryPolicy>()
    
    val processor = BatchResultProcessor(validator, converter, assembler, retryPolicy)

    val posts = listOf(
        createMockPost("1"),
        createMockPost("2")
    )
    val request = BatchRequest("req-1", posts, 100, 0)

    test("processBatchResponse should process successful and failed results correctly") {
        val summaryResponse = BatchSummaryResponse(
            results = listOf(
                SummaryResultWithId("1", true, "Summary 1", listOf("AI"), "Preview 1", null),
                SummaryResultWithId("2", false, null, null, null, "Error")
            )
        )
        
        every { validator.validate("1", any(), any(), any()) } returns ValidationResult(true, emptyList())
        every { converter.convert(posts[0], any()) } returns posts[0].copy(isSummary = true)
        
        every { assembler.assembleResult(any(), any(), any(), any()) } answers {
            val successList = arg<List<PostDto>>(1)
            val failureList = arg<List<com.techinsights.batch.dto.BatchFailure>>(2)
            
            mockk<BatchResult> {
                every { successes } returns successList
                every { failures } returns failureList
            }
        }

        val result = processor.processBatchResponse(request, summaryResponse, System.currentTimeMillis())

        result.successes.size shouldBe 1
        result.failures.size shouldBe 1
        result.failures[0].post.id shouldBe "2"
        result.failures[0].errorType shouldBe ErrorType.VALIDATION_ERROR
    }

    test("processBatchResponse should handle validation failure") {
        val summaryResponse = BatchSummaryResponse(
            results = listOf(
                SummaryResultWithId("1", true, "Summary 1", null, null, null)
            )
        )
        
        every { validator.validate("1", any(), any(), any()) } returns ValidationResult(false, listOf("Invalid content"))
        
        every { assembler.assembleResult(any(), any(), any(), any()) } answers {
            val failureList = arg<List<com.techinsights.batch.dto.BatchFailure>>(2)
            mockk { every { failures } returns failureList }
        }

        val result = processor.processBatchResponse(request.copy(posts = listOf(posts[0])), summaryResponse, System.currentTimeMillis())

        result.failures.size shouldBe 1
        result.failures[0].reason shouldBe "Invalid content"
    }
    
    test("createFailureResult should delegate to assembler with correct retry policy") {
        every { retryPolicy.isRetryableError(ErrorType.TIMEOUT) } returns true
        every { assembler.assembleFailureResult(any(), any(), any(), any()) } returns mockk()

        processor.createFailureResult(request, "Timeout", ErrorType.TIMEOUT)

        verify { assembler.assembleFailureResult(request, "Timeout", ErrorType.TIMEOUT, true) }
    }
})

private fun createMockPost(id: String): PostDto {
    return PostDto(
        id = id,
        title = "Title $id",
        content = "Content $id",
        url = "http://url.com/$id",
        publishedAt = LocalDateTime.now(),
        company = mockk(),
        isSummary = false,
        preview = null,
        categories = emptySet(),
        isEmbedding = false
    )
}
