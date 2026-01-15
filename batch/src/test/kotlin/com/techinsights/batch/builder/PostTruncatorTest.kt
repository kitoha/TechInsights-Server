package com.techinsights.batch.builder

import com.techinsights.batch.config.BatchBuildConfig
import com.techinsights.domain.dto.post.PostDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.mockk.mockk
import java.time.LocalDateTime

class PostTruncatorTest : FunSpec({

    val config = BatchBuildConfig(
        maxTokensPerRequest = 1000,
        truncationBufferTokens = 200,
        tokensPerChar = 4
    )
    val truncator = PostTruncator(config)

    test("truncate should truncate content exceeding max tokens") {
        val longContent = "a".repeat(250)
        val post = createMockPost(longContent)

        val truncated = truncator.truncate(post, 800)

        truncated.content.length shouldBe 200 + "\n\n[내용이 잘렸습니다]".length
        truncated.content shouldEndWith "[내용이 잘렸습니다]"
    }

    test("truncate should not change content within limit") {
        val shortContent = "a".repeat(100)
        val post = createMockPost(shortContent)

        val truncated = truncator.truncate(post, 800)

        truncated.content shouldBe shortContent
    }

    test("calculateMaxTokensForTruncation should use config") {
        truncator.calculateMaxTokensForTruncation() shouldBe 800
    }
})

private fun createMockPost(content: String): PostDto {
    return PostDto(
        id = "1",
        title = "T",
        content = content,
        url = "U",
        publishedAt = LocalDateTime.now(),
        company = mockk(),
        isSummary = false,
        preview = null,
        categories = emptySet(),
        isEmbedding = false
    )
}
