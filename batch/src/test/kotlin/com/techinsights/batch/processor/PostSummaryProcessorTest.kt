package com.techinsights.batch.processor

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.gemini.SummaryResult
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.service.gemini.ArticleSummarizer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

class PostSummaryProcessorTest : FunSpec({

    val summarizer = mockk<ArticleSummarizer>()
    val processor = PostSummaryProcessor(summarizer)

    beforeEach {
        clearAllMocks()
    }

    test("process should successfully summarize post and update fields") {
        // given
        val originalPost = createMockPostDto(
            id = "1",
            content = "Original long content about Spring Boot",
            preview = "Original preview",
            categories = emptySet(),
            isSummary = false
        )

        val summaryResult = SummaryResult(
            summary = "Summarized content about Spring Boot microservices",
            preview = "Summarized preview text",
            categories = listOf("BackEnd", "Infra")
        )

        coEvery {
            summarizer.summarize(originalPost.content, GeminiModelType.GEMINI_2_5_FLASH_LITE)
        } returns summaryResult

        // when
        val result = processor.process(originalPost)

        // then
        result shouldBe originalPost.copy(
            content = summaryResult.summary,
            preview = summaryResult.preview,
            categories = setOf(Category.BackEnd, Category.Infra),
            isSummary = true
        )

        coVerify(exactly = 1) {
            summarizer.summarize(originalPost.content, GeminiModelType.GEMINI_2_5_FLASH_LITE)
        }
    }

    test("process should handle single category") {
        // given
        val originalPost = createMockPostDto(
            id = "2",
            content = "AI and machine learning article",
            isSummary = false
        )

        val summaryResult = SummaryResult(
            summary = "AI summary",
            preview = "AI preview",
            categories = listOf("AI")
        )

        coEvery {
            summarizer.summarize(any(), any())
        } returns summaryResult

        // when
        val result = processor.process(originalPost)

        // then
        result.categories shouldBe setOf(Category.AI)
        result.isSummary shouldBe true
    }

    test("process should handle multiple categories") {
        // given
        val originalPost = createMockPostDto(
            id = "3",
            content = "Full stack development guide",
            isSummary = false
        )

        val summaryResult = SummaryResult(
            summary = "Full stack summary",
            preview = "Full stack preview",
            categories = listOf("FrontEnd", "BackEnd", "Infra")
        )

        coEvery {
            summarizer.summarize(any(), any())
        } returns summaryResult

        // when
        val result = processor.process(originalPost)

        // then
        result.categories shouldBe setOf(Category.FrontEnd, Category.BackEnd, Category.Infra)
        result.isSummary shouldBe true
    }

    test("process should return original post when summarization fails") {
        // given
        val originalPost = createMockPostDto(
            id = "4",
            content = "Test content",
            preview = "Test preview",
            categories = setOf(Category.AI),
            isSummary = false
        )

        coEvery {
            summarizer.summarize(any(), any())
        } throws RuntimeException("Gemini API error")

        // when
        val result = processor.process(originalPost)

        // then
        result shouldBe originalPost
        result.isSummary shouldBe false
        result.content shouldBe "Test content"
        result.preview shouldBe "Test preview"
        result.categories shouldBe setOf(Category.AI)
    }

    test("process should handle IllegalArgumentException") {
        // given
        val originalPost = createMockPostDto(
            id = "5",
            content = "Test content",
            isSummary = false
        )

        coEvery {
            summarizer.summarize(any(), any())
        } throws IllegalArgumentException("Invalid category")

        // when
        val result = processor.process(originalPost)

        // then
        result shouldBe originalPost
        coVerify(exactly = 1) { summarizer.summarize(any(), any()) }
    }

    test("process should preserve other post fields") {
        // given
        val company = CompanyDto(
            id = "company1",
            name = "Test Company",
            blogUrl = "https://test.com",
            logoImageName = "logo.png",
            rssSupported = true
        )

        val originalPost = PostDto(
            id = "6",
            title = "Original Title",
            preview = "Original preview",
            url = "https://test.com/post",
            content = "Original content",
            publishedAt = LocalDateTime.of(2024, 1, 1, 12, 0),
            thumbnail = "thumbnail.jpg",
            company = company,
            viewCount = 100,
            categories = emptySet(),
            isSummary = false,
            isEmbedding = false
        )

        val summaryResult = SummaryResult(
            summary = "New summary",
            preview = "New preview",
            categories = listOf("AI")
        )

        coEvery {
            summarizer.summarize(any(), any())
        } returns summaryResult

        // when
        val result = processor.process(originalPost)

        // then
        result.id shouldBe "6"
        result.title shouldBe "Original Title"
        result.url shouldBe "https://test.com/post"
        result.publishedAt shouldBe LocalDateTime.of(2024, 1, 1, 12, 0)
        result.thumbnail shouldBe "thumbnail.jpg"
        result.company shouldBe company
        result.viewCount shouldBe 100
        result.isEmbedding shouldBe false
        // Updated fields
        result.content shouldBe "New summary"
        result.preview shouldBe "New preview"
        result.categories shouldBe setOf(Category.AI)
        result.isSummary shouldBe true
    }

    test("process should handle empty content") {
        // given
        val originalPost = createMockPostDto(
            id = "7",
            content = "",
            isSummary = false
        )

        val summaryResult = SummaryResult(
            summary = "Generated summary",
            preview = "Generated preview",
            categories = listOf("BackEnd")
        )

        coEvery {
            summarizer.summarize("", any())
        } returns summaryResult

        // when
        val result = processor.process(originalPost)

        // then
        result.content shouldBe "Generated summary"
        result.isSummary shouldBe true
    }

    test("process should use GEMINI_2_5_FLASH_LITE model") {
        // given
        val originalPost = createMockPostDto(
            id = "8",
            content = "Test content",
            isSummary = false
        )

        val summaryResult = SummaryResult(
            summary = "Summary",
            preview = "Preview",
            categories = listOf("AI")
        )

        coEvery {
            summarizer.summarize(any(), GeminiModelType.GEMINI_2_5_FLASH_LITE)
        } returns summaryResult

        // when
        processor.process(originalPost)

        // then
        coVerify(exactly = 1) {
            summarizer.summarize(any(), GeminiModelType.GEMINI_2_5_FLASH_LITE)
        }
    }

    test("process should handle very long content") {
        // given
        val longContent = "A".repeat(50000)
        val originalPost = createMockPostDto(
            id = "9",
            content = longContent,
            isSummary = false
        )

        val summaryResult = SummaryResult(
            summary = "Concise summary of long content",
            preview = "Short preview",
            categories = listOf("BackEnd")
        )

        coEvery {
            summarizer.summarize(longContent, any())
        } returns summaryResult

        // when
        val result = processor.process(originalPost)

        // then
        result.content shouldBe "Concise summary of long content"
        assert(result.content.length < longContent.length) // Summary is shorter than original
    }

    test("process should handle all valid categories") {
        // given
        val originalPost = createMockPostDto(
            id = "10",
            content = "Multi-category content",
            isSummary = false
        )

        val summaryResult = SummaryResult(
            summary = "Summary",
            preview = "Preview",
            categories = listOf("AI", "BackEnd", "FrontEnd", "BigData", "Infra", "Architecture")
        )

        coEvery {
            summarizer.summarize(any(), any())
        } returns summaryResult

        // when
        val result = processor.process(originalPost)

        // then
        result.categories shouldBe setOf(
            Category.AI,
            Category.BackEnd,
            Category.FrontEnd,
            Category.BigData,
            Category.Infra,
            Category.Architecture
        )
    }

    test("process should handle network timeout exception") {
        // given
        val originalPost = createMockPostDto(
            id = "11",
            content = "Test content",
            isSummary = false
        )

        coEvery {
            summarizer.summarize(any(), any())
        } throws java.net.SocketTimeoutException("Connection timeout")

        // when
        val result = processor.process(originalPost)

        // then
        result shouldBe originalPost
    }
})

private fun createMockPostDto(
    id: String,
    content: String = "Test content",
    preview: String = "Test preview",
    categories: Set<Category> = emptySet(),
    isSummary: Boolean = false
): PostDto {
    val company = CompanyDto(
        id = "company1",
        name = "Test Company",
        blogUrl = "https://test.com",
        logoImageName = "logo.png",
        rssSupported = true
    )

    return PostDto(
        id = id,
        title = "Test Post",
        preview = preview,
        url = "https://test.com/post/$id",
        content = content,
        publishedAt = LocalDateTime.now(),
        thumbnail = null,
        company = company,
        viewCount = 0,
        categories = categories,
        isSummary = isSummary,
        isEmbedding = false
    )
}
