package com.techinsights.batch.summary.processor

import com.techinsights.batch.summary.dto.BatchFailure
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.ErrorType
import com.techinsights.domain.enums.SummaryErrorType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.time.LocalDateTime

class FailurePostMapperTest : FunSpec({

    val mapper = FailurePostMapper()

    test("mapFailuresToPosts should filter successful posts and map failures") {
        val post1 = createMockPost("1") // Success
        val post2 = createMockPost("2") // Failure
        val post3 = createMockPost("3") // Failure without record (edge case)

        val originalPosts = listOf(post1, post2, post3)
        val successes = listOf(post1)
        
        val failures = listOf(
            BatchFailure(post2, "Validation Error", true, ErrorType.VALIDATION_ERROR)
        )

        val result = mapper.mapFailuresToPosts(originalPosts, successes, failures)

        result shouldHaveSize 2

        val mappedPost2 = result.find { it.id == "2" }!!
        mappedPost2.failureErrorMessage shouldBe "Validation Error"
        mappedPost2.failureErrorType shouldBe SummaryErrorType.VALIDATION_ERROR
        mappedPost2.failureIsBatchFailure shouldBe false

        val mappedPost3 = result.find { it.id == "3" }!!
        mappedPost3.failureErrorMessage shouldBe null
    }

    test("mapFailuresToPosts should leave quota-exhausted posts out so they are not counted as failures") {
        val exhausted = createMockPost("1")
        val failed = createMockPost("2")

        val result = mapper.mapFailuresToPosts(
            originalPosts = listOf(exhausted, failed),
            successes = emptyList(),
            failures = listOf(
                BatchFailure(exhausted, "Daily Gemini quota exhausted", false, ErrorType.QUOTA_EXHAUSTED),
                BatchFailure(failed, "Validation Error", true, ErrorType.VALIDATION_ERROR)
            )
        )

        result.map { it.id } shouldBe listOf("2")
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
