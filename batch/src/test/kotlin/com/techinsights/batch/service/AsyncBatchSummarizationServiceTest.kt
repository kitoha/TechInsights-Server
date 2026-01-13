package com.techinsights.batch.service

import com.techinsights.batch.dto.BatchRequest
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.BatchSummaryResponse
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.enums.ErrorType
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.service.gemini.BatchArticleSummarizer
import com.techinsights.domain.service.gemini.BatchSummaryValidator
import com.techinsights.domain.service.gemini.BatchSummaryValidator.ValidationResult
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import java.time.LocalDateTime

class AsyncBatchSummarizationServiceTest : FunSpec({

    lateinit var batchSummarizer: BatchArticleSummarizer
    lateinit var validator: BatchSummaryValidator
    lateinit var circuitBreakerRegistry: CircuitBreakerRegistry
    lateinit var circuitBreaker: CircuitBreaker
    lateinit var service: AsyncBatchSummarizationService
    val testDispatcher = StandardTestDispatcher()

    beforeEach {
        batchSummarizer = mockk()
        validator = mockk()
        circuitBreakerRegistry = mockk()
        circuitBreaker = mockk()

        every { circuitBreakerRegistry.circuitBreaker("geminiBatch") } returns circuitBreaker
        every { circuitBreaker.state } returns CircuitBreaker.State.CLOSED

        service = AsyncBatchSummarizationService(
            batchSummarizer,
            validator,
            circuitBreakerRegistry,
            Dispatchers.Unconfined
        )
    }

    afterEach {
        clearAllMocks()
    }

    test("processBatchesAsync should successfully process single batch") {
        // given
        val post = createPostDto("1", "Title", "Content")
        val batchRequest = BatchRequest("batch-1", listOf(post), 1000, 0)

        val batchResponse = BatchSummaryResponse(
            listOf(
                SummaryResultWithId(
                    id = "1",
                    success = true,
                    summary = "Summarized content",
                    preview = "Preview",
                    categories = listOf("AI"),
                    error = null
                )
            )
        )

        coEvery { batchSummarizer.summarizeBatch(any(), GeminiModelType.GEMINI_2_5_FLASH_LITE) } returns batchResponse
        every { validator.validate(any(), any(), any()) } returns ValidationResult(true, emptyList())

        // when
        val results = service.processBatchesAsync(listOf(batchRequest))

        // then
        results shouldHaveSize 1
        results[0].successes shouldHaveSize 1
        results[0].failures.shouldBeEmpty()
        results[0].successes[0].isSummary shouldBe true
        results[0].successes[0].content shouldBe "Summarized content"
        results[0].successes[0].preview shouldBe "Preview"
        results[0].successes[0].categories shouldBe setOf(Category.AI)

        coVerify(exactly = 1) { batchSummarizer.summarizeBatch(any(), any()) }
    }

    test("processBatchesAsync should handle validation failure") {
        // given
        val post = createPostDto("1", "Title", "Content")
        val batchRequest = BatchRequest("batch-1", listOf(post), 1000, 0)

        val batchResponse = BatchSummaryResponse(
            listOf(
                SummaryResultWithId(
                    id = "1",
                    success = true,
                    summary = "Bad summary",
                    preview = null,
                    categories = null,
                    error = null
                )
            )
        )

        coEvery { batchSummarizer.summarizeBatch(any(), any()) } returns batchResponse
        every { validator.validate(any(), any(), any()) } returns ValidationResult(
            false,
            listOf("Preview is missing")
        )

        // when
        val results = service.processBatchesAsync(listOf(batchRequest))

        // then
        results shouldHaveSize 1
        results[0].successes.shouldBeEmpty()
        results[0].failures shouldHaveSize 1
        results[0].failures[0].reason shouldBe "Preview is missing"
        results[0].failures[0].retryable shouldBe true
    }

    test("processBatchesAsync should handle circuit breaker open state") {
        // given
        every { circuitBreaker.state } returns CircuitBreaker.State.OPEN
        val post = createPostDto("1", "Title", "Content")
        val batchRequest = BatchRequest("batch-1", listOf(post), 1000, 0)

        // when
        val results = service.processBatchesAsync(listOf(batchRequest))

        // then
        results shouldHaveSize 1
        results[0].successes.shouldBeEmpty()
        results[0].failures shouldHaveSize 1
        results[0].failures[0].reason shouldBe "Circuit breaker open - API unavailable"
        results[0].failures[0].errorType shouldBe ErrorType.API_ERROR

        coVerify(exactly = 0) { batchSummarizer.summarizeBatch(any(), any()) }
    }

    test("processBatchesAsync should handle API result without matching post") {
        // given
        val post = createPostDto("1", "Title", "Content")
        val batchRequest = BatchRequest("batch-1", listOf(post), 1000, 0)

        val batchResponse = BatchSummaryResponse(listOf()) // No results

        coEvery { batchSummarizer.summarizeBatch(any(), any()) } returns batchResponse

        // when
        val results = service.processBatchesAsync(listOf(batchRequest))

        // then
        results shouldHaveSize 1
        results[0].successes.shouldBeEmpty()
        results[0].failures shouldHaveSize 1
        results[0].failures[0].reason shouldBe "No result returned for this post"
        results[0].failures[0].retryable shouldBe false
    }

    test("processBatchesAsync should handle API result with success false") {
        // given
        val post = createPostDto("1", "Title", "Content")
        val batchRequest = BatchRequest("batch-1", listOf(post), 1000, 0)

        val batchResponse = BatchSummaryResponse(
            listOf(
                SummaryResultWithId(
                    id = "1",
                    success = false,
                    summary = null,
                    preview = null,
                    categories = null,
                    error = "API processing error"
                )
            )
        )

        coEvery { batchSummarizer.summarizeBatch(any(), any()) } returns batchResponse

        // when
        val results = service.processBatchesAsync(listOf(batchRequest))

        // then
        results shouldHaveSize 1
        results[0].successes.shouldBeEmpty()
        results[0].failures shouldHaveSize 1
        results[0].failures[0].reason shouldBe "API processing error"
        results[0].failures[0].retryable shouldBe true
    }

    test("processBatchesAsync should process multiple batches in parallel") {
        // given
        val post1 = createPostDto("1", "Title 1", "Content 1")
        val post2 = createPostDto("2", "Title 2", "Content 2")
        val batch1 = BatchRequest("batch-1", listOf(post1), 1000, 0)
        val batch2 = BatchRequest("batch-2", listOf(post2), 1000, 0)

        val response1 = BatchSummaryResponse(
            listOf(
                SummaryResultWithId(
                    id = "1",
                    success = true,
                    summary = "Summary 1",
                    preview = "Preview 1",
                    categories = listOf("AI"),
                    error = null
                )
            )
        )

        val response2 = BatchSummaryResponse(
            listOf(
                SummaryResultWithId(
                    id = "2",
                    success = true,
                    summary = "Summary 2",
                    preview = "Preview 2",
                    categories = listOf("Backend"),
                    error = null
                )
            )
        )

        coEvery { batchSummarizer.summarizeBatch(any(), any()) } returnsMany listOf(response1, response2)
        every { validator.validate(any(), any(), any()) } returns ValidationResult(true, emptyList())

        // when
        val results = service.processBatchesAsync(listOf(batch1, batch2))

        // then
        results shouldHaveSize 2
        results[0].successes shouldHaveSize 1
        results[1].successes shouldHaveSize 1

        coVerify(exactly = 2) { batchSummarizer.summarizeBatch(any(), any()) }
    }

    test("processBatchesAsync should sort batches by priority") {
        // given
        val post1 = createPostDto("1", "Title 1", "Content 1")
        val post2 = createPostDto("2", "Title 2", "Content 2")
        val batch1 = BatchRequest("batch-1", listOf(post1), 1000, 1)
        val batch2 = BatchRequest("batch-2", listOf(post2), 1000, 2)

        val response = BatchSummaryResponse(
            listOf(
                SummaryResultWithId(
                    id = "1",
                    success = true,
                    summary = "Summary",
                    preview = "Preview",
                    categories = listOf("AI"),
                    error = null
                )
            )
        )

        coEvery { batchSummarizer.summarizeBatch(any(), any()) } returns response
        every { validator.validate(any(), any(), any()) } returns ValidationResult(true, emptyList())

        // when
        service.processBatchesAsync(listOf(batch1, batch2))

        // then - verify calls are made (priority 2 should be processed first)
        coVerify(exactly = 2) { batchSummarizer.summarizeBatch(any(), any()) }
    }

    test("processBatchesAsync should handle invalid categories") {
        // given
        val post = createPostDto("1", "Title", "Content")
        val batchRequest = BatchRequest("batch-1", listOf(post), 1000, 0)

        val batchResponse = BatchSummaryResponse(
            listOf(
                SummaryResultWithId(
                    id = "1",
                    success = true,
                    summary = "Summary",
                    preview = "Preview",
                    categories = listOf("INVALID_CATEGORY"),
                    error = null
                )
            )
        )

        coEvery { batchSummarizer.summarizeBatch(any(), any()) } returns batchResponse
        every { validator.validate(any(), any(), any()) } returns ValidationResult(true, emptyList())

        // when
        val results = service.processBatchesAsync(listOf(batchRequest))

        // then
        results shouldHaveSize 1
        results[0].successes shouldHaveSize 1
        results[0].successes[0].categories.shouldBeEmpty() // Invalid categories are filtered out
    }
})

private fun createPostDto(
    id: String,
    title: String,
    content: String
): PostDto {
    return PostDto(
        id = id,
        title = title,
        content = content,
        url = "https://example.com/$id",
        publishedAt = LocalDateTime.now(),
        company = CompanyDto(
            id = "company-1",
            name = "Test Company",
            
            logoImageName = "",
            blogUrl = "https://example.com/rss"
        ),
        isSummary = false,
        preview = null,
        categories = emptySet(),
        isEmbedding = false
    )
}
