package com.techinsights.batch.summary.processor

import com.techinsights.batch.summary.builder.DynamicBatchBuilder
import com.techinsights.batch.summary.dto.BatchFailure
import com.techinsights.batch.summary.dto.BatchMetrics
import com.techinsights.batch.summary.dto.BatchResult
import com.techinsights.batch.summary.service.AsyncBatchSummarizationService
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.ErrorType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

class AsyncBatchPostSummaryProcessorTest : FunSpec({

    lateinit var batchService: AsyncBatchSummarizationService
    lateinit var batchBuilder: DynamicBatchBuilder
    lateinit var failurePostMapper: FailurePostMapper
    lateinit var processor: AsyncBatchPostSummaryProcessor

    beforeEach {
        batchService = mockk()
        batchBuilder = mockk()
        failurePostMapper = mockk()

        every { failurePostMapper.mapFailuresToPosts(any(), any(), any()) } returns emptyList()
        
        processor = AsyncBatchPostSummaryProcessor(batchService, batchBuilder, failurePostMapper)
    }

    afterEach {
        clearAllMocks()
    }

    test("process should return null for empty list") {
        // when
        val result = processor.process(emptyList())

        // then
        result.shouldBeNull()
        verify(exactly = 0) { batchBuilder.buildBatches(any()) }
    }

    test("process should successfully process posts and return successes") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", "Content 1"),
            createPostDto("2", "Title 2", "Content 2")
        )

        val batch = DynamicBatchBuilder.Batch(posts, 1000)

        val batchResult = BatchResult(
            requestId = "test-batch-id",
            successes = posts.map { it.copy(isSummary = true) },
            failures = emptyList(),
            metrics = BatchMetrics(2, 2, 0, 1, 1000, 100)
        )

        every { batchBuilder.buildBatches(posts) } returns listOf(batch)
        coEvery { batchService.processBatchesAsync(any()) } returns listOf(batchResult)

        // when
        val result = processor.process(posts)

        // then
        result.shouldNotBeNull()
        result shouldHaveSize 2
        result.forEach { it.isSummary shouldBe true }

        verify(exactly = 1) { batchBuilder.buildBatches(posts) }
        coVerify(exactly = 1) { batchService.processBatchesAsync(any()) }
    }

    test("process should handle partial failures and return all posts") {
        // given
        val post1 = createPostDto("1", "Title 1", "Content 1")
        val post2 = createPostDto("2", "Title 2", "Content 2")
        val posts = listOf(post1, post2)

        val batch = DynamicBatchBuilder.Batch(posts, 1000)

        val batchResult = BatchResult(
            requestId = "test-batch-id",
            successes = listOf(post1.copy(isSummary = true)),
            failures = listOf(
                BatchFailure(
                    post = post2,
                    reason = "Validation failed",
                    retryable = true,
                    errorType = ErrorType.VALIDATION_ERROR
                )
            ),
            metrics = BatchMetrics(2, 1, 1, 1, 1000, 100)
        )

        every { batchBuilder.buildBatches(posts) } returns listOf(batch)
        coEvery { batchService.processBatchesAsync(any()) } returns listOf(batchResult)
        every { failurePostMapper.mapFailuresToPosts(any(), any(), any()) } returns listOf(post2)

        // when
        val result = processor.process(posts)

        // then
        result.shouldNotBeNull()
        result shouldHaveSize 2
        result.count { it.isSummary } shouldBe 1
        result.map { it.id } shouldContainAll listOf("1", "2")
    }

    test("process should handle multiple batches") {
        // given
        val posts = (1..5).map { createPostDto("$it", "Title $it", "Content $it") }
        val batch1 = DynamicBatchBuilder.Batch(posts.take(3), 1500)
        val batch2 = DynamicBatchBuilder.Batch(posts.drop(3), 1000)

        val batchResult1 = BatchResult(
            requestId = "batch-1",
            successes = posts.take(3).map { it.copy(isSummary = true) },
            failures = emptyList(),
            metrics = BatchMetrics(3, 3, 0, 1, 1500, 100)
        )

        val batchResult2 = BatchResult(
            requestId = "batch-2",
            successes = posts.drop(3).map { it.copy(isSummary = true) },
            failures = emptyList(),
            metrics = BatchMetrics(2, 2, 0, 1, 1000, 100)
        )

        every { batchBuilder.buildBatches(posts) } returns listOf(batch1, batch2)
        coEvery { batchService.processBatchesAsync(any()) } returns listOf(batchResult1, batchResult2)

        // when
        val result = processor.process(posts)

        // then
        result.shouldNotBeNull()
        result shouldHaveSize 5
        result.all { it.isSummary } shouldBe true
    }

    test("process should handle complete batch failure") {
        // given
        val posts = listOf(createPostDto("1", "Title", "Content"))
        val batch = DynamicBatchBuilder.Batch(posts, 1000)

        val batchResult = BatchResult(
            requestId = "test-batch-id",
            successes = emptyList(),
            failures = listOf(
                BatchFailure(
                    post = posts[0],
                    reason = "API error",
                    retryable = false,
                    errorType = ErrorType.API_ERROR
                )
            ),
            metrics = BatchMetrics(1, 0, 1, 0, 0, 100)
        )

        every { batchBuilder.buildBatches(posts) } returns listOf(batch)
        coEvery { batchService.processBatchesAsync(any()) } returns listOf(batchResult)
        every { failurePostMapper.mapFailuresToPosts(any(), any(), any()) } returns listOf(posts[0])

        // when
        val result = processor.process(posts)

        // then
        result.shouldNotBeNull()
        result shouldHaveSize 1
        result[0].isSummary shouldBe false
        result[0].id shouldBe "1"
    }

    test("process should create correct BatchRequest from DynamicBatchBuilder.Batch") {
        // given
        val posts = listOf(createPostDto("1", "Title", "Content"))
        val batch = DynamicBatchBuilder.Batch(posts, 2000)

        val batchResult = BatchResult(
            requestId = "test-id",
            successes = posts.map { it.copy(isSummary = true) },
            failures = emptyList(),
            metrics = BatchMetrics(1, 1, 0, 1, 2000, 50)
        )

        every { batchBuilder.buildBatches(posts) } returns listOf(batch)
        coEvery { batchService.processBatchesAsync(match { requests ->
            requests.size == 1 &&
            requests[0].posts == posts &&
            requests[0].estimatedTokens == 2000 &&
            requests[0].priority == 0
        }) } returns listOf(batchResult)

        // when
        processor.process(posts)

        // then
        coVerify(exactly = 1) { batchService.processBatchesAsync(any()) }
    }
})

private fun createPostDto(
    id: String,
    title: String,
    content: String,
    isSummary: Boolean = false
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
            blogUrl = "https://example.com/rss",
            logoImageName = ""
        ),
        isSummary = isSummary,
        preview = null,
        categories = emptySet(),
        isEmbedding = false
    )
}
