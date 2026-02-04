package com.techinsights.batch.processor

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

    test("process should successfully generate embeddings for valid posts") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", "Preview 1", isSummary = true),
            createPostDto("2", "Title 2", "Preview 2", isSummary = true)
        )

        val embeddingResults = posts.map { post ->
            EmbeddingService.EmbeddingResult(
                request = EmbeddingRequest(
                    content = post.preview!!,
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
        result[0].content shouldBe "Preview 1"
        result[0].embeddingVector.size shouldBe 768
        result[1].postId shouldBe "2"
        result[1].content shouldBe "Preview 2"

        verify(exactly = 1) { embeddingService.generateEmbeddingBatch(any(), GeminiModelType.GEMINI_EMBEDDING) }
    }

    test("process should filter out posts that are not summarized") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", "Preview 1", isSummary = false),
            createPostDto("2", "Title 2", "Preview 2", isSummary = true)
        )

        val embeddingResult = EmbeddingService.EmbeddingResult(
            request = EmbeddingRequest(
                content = "Preview 2",
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

    test("process should filter out posts with null preview") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", null, isSummary = true),
            createPostDto("2", "Title 2", "Preview 2", isSummary = true)
        )

        val embeddingResult = EmbeddingService.EmbeddingResult(
            request = EmbeddingRequest("Preview 2", emptyList(), "Test Company"),
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

    test("process should filter out posts with blank preview") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", "", isSummary = true),
            createPostDto("2", "Title 2", "Preview 2", isSummary = true)
        )

        val embeddingResult = EmbeddingService.EmbeddingResult(
            request = EmbeddingRequest("Preview 2", emptyList(), "Test Company"),
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
            createPostDto("1", "Title 1", null, isSummary = false),
            createPostDto("2", "Title 2", "", isSummary = true)
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
            createPostDto("1", "Title 1", "Preview 1", isSummary = true),
            createPostDto("2", "Title 2", "Preview 2", isSummary = true)
        )

        val embeddingResults = listOf(
            EmbeddingService.EmbeddingResult(
                request = EmbeddingRequest("Preview 1", emptyList(), "Test Company"),
                vector = List(768) { it.toFloat() },
                success = true
            ),
            EmbeddingService.EmbeddingResult(
                request = EmbeddingRequest("Preview 2", emptyList(), "Test Company"),
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
            createPostDto("1", "Title 1", "Preview 1", isSummary = true)
        )

        val embeddingResult = EmbeddingService.EmbeddingResult(
            request = EmbeddingRequest("Preview 1", emptyList(), "Test Company"),
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

    test("process should include categories in embedding request") {
        // given
        val posts = listOf(
            createPostDto("1", "Title 1", "Preview 1", isSummary = true, categories = setOf(Category.AI, Category.BackEnd))
        )

        val embeddingResult = EmbeddingService.EmbeddingResult(
            request = EmbeddingRequest("Preview 1", listOf("AI", "BACKEND"), "Test Company"),
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
            createPostDto("1", "Title 1", "Preview 1", isSummary = true)
        )

        every { embeddingService.generateEmbeddingBatch(any(), any()) } throws RuntimeException("Service error")

        // when / then
        val exception = runCatching { processor.process(posts) }.exceptionOrNull()
        exception.shouldBeInstanceOf<RuntimeException>()
    }

    test("process should create correct EmbeddingRequest with all fields") {
        // given
        val posts = listOf(
            createPostDto(
                id = "1",
                title = "Title",
                preview = "Test preview content",
                isSummary = true,
                categories = setOf(Category.AI),
                companyName = "Tech Company"
            )
        )

        val embeddingResult = EmbeddingService.EmbeddingResult(
            request = EmbeddingRequest("Test preview content", listOf("AI"), "Tech Company"),
            vector = List(768) { it.toFloat() },
            success = true
        )

        every { embeddingService.generateEmbeddingBatch(any(), any()) } returns listOf(embeddingResult)

        // when
        processor.process(posts)

        // then
        verify(exactly = 1) {
            embeddingService.generateEmbeddingBatch(
                match { requests ->
                    requests[0].content == "Test preview content" &&
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
    preview: String?,
    isSummary: Boolean = false,
    categories: Set<Category> = emptySet(),
    companyName: String = "Test Company"
): PostDto {
    return PostDto(
        id = id,
        title = title,
        content = "Content",
        url = "https://example.com/$id",
        publishedAt = LocalDateTime.now(),
        company = CompanyDto(
            id = "company-1",
            name = companyName,
            blogUrl = "https://example.com/rss",
            logoImageName = ""
        ),
        isSummary = isSummary,
        preview = preview,
        categories = categories,
        isEmbedding = false
    )
}
