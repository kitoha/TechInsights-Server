package com.techinsights.batch.service

import com.techinsights.batch.dto.BatchFailure
import com.techinsights.batch.dto.BatchRequest
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.ErrorType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.time.LocalDateTime

class BatchResultAssemblerTest : FunSpec({

    val assembler = BatchResultAssembler()

    val posts = listOf(
        createMockPost("1"),
        createMockPost("2")
    )
    val request = BatchRequest(
        id = "req-1",
        posts = posts,
        estimatedTokens = 100,
        priority = 0
    )

    test("assembleResult should create correct result with mixed success and failure") {
        val successes = listOf(posts[0])
        val failures = listOf(
            BatchFailure(posts[1], "Error", true, ErrorType.API_ERROR)
        )
        val duration = 500L

        val result = assembler.assembleResult(request, successes, failures, duration)

        result.requestId shouldBe "req-1"
        result.successes shouldBe successes
        result.failures shouldBe failures
        result.metrics.totalItems shouldBe 2
        result.metrics.successCount shouldBe 1
        result.metrics.failureCount shouldBe 1
        result.metrics.durationMs shouldBe 500L
        result.metrics.apiCallCount shouldBe 1
    }

    test("assembleFailureResult should create correct result for complete failure") {
        val reason = "Batch Failed"
        val errorType = ErrorType.TIMEOUT

        val result = assembler.assembleFailureResult(request, reason, errorType, true)

        result.requestId shouldBe "req-1"
        result.successes.isEmpty() shouldBe true
        result.failures.size shouldBe 2
        result.failures[0].reason shouldBe reason
        result.failures[0].errorType shouldBe errorType
        
        result.metrics.totalItems shouldBe 2
        result.metrics.successCount shouldBe 0
        result.metrics.failureCount shouldBe 2
        result.metrics.apiCallCount shouldBe 0
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
