package com.techinsights.batch.embedding.processor

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.embedding.EmbeddingRequest
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.service.embedding.EmbeddingService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

class BatchPostEmbeddingProcessorTest : FunSpec({

    lateinit var embeddingService: EmbeddingService
    lateinit var processor: BatchPostEmbeddingProcessor

    beforeEach {
        embeddingService = mockk()
        processor = BatchPostEmbeddingProcessor(embeddingService)
    }

    afterEach {
        clearAllMocks()
    }

    test("process should return null for empty list") {
        // when
        val result = processor.process(emptyList())

        // then
        result.shouldBeNull()
        verify(exactly = 0) { embeddingService.generateEmbeddingBatch(any(), any()) }
    }

    test("process should successfully generate embeddings using content field") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", content = "Detailed content 1", isSummary = true),
            createPostDto("2", "Title 2", content = "Detailed content 2", isSummary = true)
        )

        val embeddingResults = posts.map { post ->
            EmbeddingService.EmbeddingResult(
                request = EmbeddingRequest(
                    content = post.content,
                    categories = post.categories.map { it.name },
                    companyName = post.company.name
                ),
                vector = List(768) { it.toFloat() },
                success = true
            )
        }

        every { embeddingService.generateEmbeddingBatch(any(), GeminiModelType.GEMINI_EMBEDDING) } returns embeddingResults

        // when
        val result = processor.process(posts)

        // then
        result.shouldNotBeNull()
        result shouldHaveSize 2
        result[0].postId shouldBe "1"
        result[0].content shouldBe "Detailed content 1"
        result[0].embeddingVector.size shouldBe 768
        result[1].postId shouldBe "2"
        result[1].content shouldBe "Detailed content 2"

        verify(exactly = 1) { embeddingService.generateEmbeddingBatch(any(), GeminiModelType.GEMINI_EMBEDDING) }
    }

    test("process should filter out posts that are not summarized") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", content = "Content 1", isSummary = false),
            createPostDto("2", "Title 2", content = "Content 2", isSummary = true)
        )

        val embeddingResult = EmbeddingService.EmbeddingResult(
            request = EmbeddingRequest(
                content = "Content 2",
                categories = emptyList(),
                companyName = "Test Company"
            ),
            vector = List(768) { it.toFloat() },
            success = true
        )

        every { embeddingService.generateEmbeddingBatch(any(), any()) } returns listOf(embeddingResult)

        // when
        val result = processor.process(posts)

        // then
        result.shouldNotBeNull()
        result shouldHaveSize 1
        result[0].postId shouldBe "2"

        verify(exactly = 1) {
            embeddingService.generateEmbeddingBatch(
                match { it.size == 1 },
                any()
            )
        }
    }

    test("process should filter out posts with blank content") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", content = "", isSummary = true),
            createPostDto("2", "Title 2", content = "Detailed content 2", isSummary = true)
        )

        val embeddingResult = EmbeddingService.EmbeddingResult(
            request = EmbeddingRequest("Detailed content 2", emptyList(), "Test Company"),
            vector = List(768) { it.toFloat() },
            success = true
        )

        every { embeddingService.generateEmbeddingBatch(any(), any()) } returns listOf(embeddingResult)

        // when
        val result = processor.process(posts)

        // then
        result.shouldNotBeNull()
        result shouldHaveSize 1
        result[0].postId shouldBe "2"
    }

    test("process should filter out posts with whitespace-only content") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", content = "   ", isSummary = true),
            createPostDto("2", "Title 2", content = "Detailed content 2", isSummary = true)
        )

        val embeddingResult = EmbeddingService.EmbeddingResult(
            request = EmbeddingRequest("Detailed content 2", emptyList(), "Test Company"),
            vector = List(768) { it.toFloat() },
            success = true
        )

        every { embeddingService.generateEmbeddingBatch(any(), any()) } returns listOf(embeddingResult)

        // when
        val result = processor.process(posts)

        // then
        result.shouldNotBeNull()
        result shouldHaveSize 1
        result[0].postId shouldBe "2"
    }

    test("process should return null when all posts are filtered out") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", content = "", isSummary = false),
            createPostDto("2", "Title 2", content = "", isSummary = true)
        )

        // when
        val result = processor.process(posts)

        // then
        result.shouldBeNull()
        verify(exactly = 0) { embeddingService.generateEmbeddingBatch(any(), any()) }
    }

    test("process should handle partial embedding failures") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", content = "Content 1", isSummary = true),
            createPostDto("2", "Title 2", content = "Content 2", isSummary = true)
        )

        val embeddingResults = listOf(
            EmbeddingService.EmbeddingResult(
                request = EmbeddingRequest("Content 1", emptyList(), "Test Company"),
                vector = List(768) { it.toFloat() },
                success = true
            ),
            EmbeddingService.EmbeddingResult(
                request = EmbeddingRequest("Content 2", emptyList(), "Test Company"),
                vector = emptyList(),
                success = false,
                error = "API error"
            )
        )

        every { embeddingService.generateEmbeddingBatch(any(), any()) } returns embeddingResults

        // when
        val result = processor.process(posts)

        // then
        result.shouldNotBeNull()
        result shouldHaveSize 1
        result[0].postId shouldBe "1"
    }

    test("process should return null when all embeddings fail") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", content = "Content 1", isSummary = true)
        )

        val embeddingResult = EmbeddingService.EmbeddingResult(
            request = EmbeddingRequest("Content 1", emptyList(), "Test Company"),
            vector = emptyList(),
            success = false,
            error = "API error"
        )

        every { embeddingService.generateEmbeddingBatch(any(), any()) } returns listOf(embeddingResult)

        // when
        val result = processor.process(posts)

        // then
        result.shouldBeNull()
    }

    test("process should include categories in embedding request using content") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", content = "Content 1", isSummary = true, categories = setOf(Category.AI, Category.BackEnd))
        )

        val embeddingResult = EmbeddingService.EmbeddingResult(
            request = EmbeddingRequest("Content 1", listOf("AI", "BackEnd"), "Test Company"),
            vector = List(768) { it.toFloat() },
            success = true
        )

        every { embeddingService.generateEmbeddingBatch(any(), any()) } returns listOf(embeddingResult)

        // when
        val result = processor.process(posts)

        // then
        result.shouldNotBeNull()
        result[0].categories shouldBe "AI,BackEnd"

        verify(exactly = 1) {
            embeddingService.generateEmbeddingBatch(
                match { requests ->
                    requests.size == 1 &&
                    requests[0].categories.containsAll(listOf("AI", "BackEnd"))
                },
                any()
            )
        }
    }

    test("process should handle exception from embedding service") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", content = "Content 1", isSummary = true)
        )

        every { embeddingService.generateEmbeddingBatch(any(), any()) } throws RuntimeException("Service error")

        // when / then
        val exception = runCatching { processor.process(posts) }.exceptionOrNull()
        exception.shouldBeInstanceOf<RuntimeException>()
    }

    test("process should create correct EmbeddingRequest with content field") {
        // given
        val posts = listOf(
            createPostDto(
                id = "1",
                title = "Title",
                content = "Markdown detailed summary content",
                isSummary = true,
                categories = setOf(Category.AI),
                companyName = "Tech Company"
            )
        )

        val embeddingResult = EmbeddingService.EmbeddingResult(
            request = EmbeddingRequest("Markdown detailed summary content", listOf("AI"), "Tech Company"),
            vector = List(768) { it.toFloat() },
            success = true
        )

        every { embeddingService.generateEmbeddingBatch(any(), any()) } returns listOf(embeddingResult)

        // when
        processor.process(posts)

        // then — content (not preview) is sent to embedding service
        verify(exactly = 1) {
            embeddingService.generateEmbeddingBatch(
                match { requests ->
                    requests[0].content == "Markdown detailed summary content" &&
                    requests[0].categories == listOf("AI") &&
                    requests[0].companyName == "Tech Company"
                },
                GeminiModelType.GEMINI_EMBEDDING
            )
        }
    }
})

private fun createPostDto(
    id: String,
    title: String,
    content: String,
    isSummary: Boolean = false,
    categories: Set<Category> = emptySet(),
    companyName: String = "Test Company"
): PostDto {
    return PostDto(
        id = id,
        title = title,
        content = content,
        url = "https://example.com/$id",
        publishedAt = LocalDateTime.now(),
        company = CompanyDto(
            id = "company-1",
            name = companyName,
            blogUrl = "https://example.com/rss",
            logoImageName = ""
        ),
        isSummary = isSummary,
        preview = null,
        categories = categories,
        isEmbedding = false
    )
}
