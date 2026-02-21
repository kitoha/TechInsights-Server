package com.techinsights.batch.summary.builder

import com.techinsights.batch.summary.config.BatchBuildConfig
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.utils.TokenEstimator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

class DynamicBatchBuilderTest : FunSpec({

    val config = BatchBuildConfig(
        basePromptTokens = 0,
        maxTokensPerRequest = 100
    )
    val limitChecker = mockk<BatchLimitChecker>()
    val postTruncator = mockk<PostTruncator>()
    
    val builder = DynamicBatchBuilder(config, limitChecker, postTruncator)

    beforeTest {
        clearAllMocks()
        mockkObject(TokenEstimator)
        every { limitChecker.maxOutputTokensAllowed } returns 1000
    }

    test("buildBatches should create single batch when limits are not exceeded") {
        val posts = listOf(createMockPost("1"), createMockPost("2"))
        
        mockTokenEstimation(posts[0], 10)
        mockTokenEstimation(posts[1], 10)
        
        every { limitChecker.exceedsMaxTokens(any()) } returns false
        every { limitChecker.exceedsInputLimit(any(), any()) } returns false
        every { limitChecker.exceedsOutputLimit(any()) } returns false
        every { limitChecker.exceedsBatchSize(any()) } returns false

        val batches = builder.buildBatches(posts)

        batches shouldHaveSize 1
        batches[0].items shouldHaveSize 2
    }

    test("buildBatches should split batches when input limit exceeded") {
        val posts = listOf(createMockPost("1"), createMockPost("2"))
        
        mockTokenEstimation(posts[0], 60)
        mockTokenEstimation(posts[1], 60)

        every { limitChecker.exceedsMaxTokens(any()) } returns false
        every { limitChecker.exceedsInputLimit(0, 60) } returns false
        every { limitChecker.exceedsInputLimit(60, 60) } returns true
        every { limitChecker.exceedsOutputLimit(any()) } returns false
        every { limitChecker.exceedsBatchSize(any()) } returns false

        val batches = builder.buildBatches(posts)

        batches shouldHaveSize 2
        batches[0].items shouldHaveSize 1
        batches[1].items shouldHaveSize 1
    }

    test("buildBatches should truncate oversized post") {
        val oversizedPost = createMockPost("big").copy(content = "Oversized Content")
        val truncatedPost = createMockPost("truncated").copy(content = "Truncated Content")
        
        mockTokenEstimation(oversizedPost, 200)
        mockTokenEstimation(truncatedPost, 50)
        
        every { limitChecker.exceedsMaxTokens(200) } returns true
        every { postTruncator.calculateMaxTokensForTruncation() } returns 90
        every { postTruncator.truncate(oversizedPost, 90) } returns truncatedPost
        
        val batches = builder.buildBatches(listOf(oversizedPost))
        
        batches shouldHaveSize 1
        batches[0].items[0] shouldBe truncatedPost
    }
})

private fun createMockPost(id: String): PostDto {
    return PostDto(
        id = id,
        title = "T",
        content = "C",
        url = "U",
        publishedAt = LocalDateTime.now(),
        company = mockk(),
        isSummary = false,
        preview = null,
        categories = emptySet(),
        isEmbedding = false
    )
}

private fun mockTokenEstimation(post: PostDto, tokens: Int) {
    every { TokenEstimator.estimateTotalTokens(post.content) } returns tokens
}
