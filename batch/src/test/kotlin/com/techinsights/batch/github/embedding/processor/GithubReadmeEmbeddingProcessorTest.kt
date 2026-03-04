package com.techinsights.batch.github.embedding.processor

import com.techinsights.domain.dto.embedding.EmbeddingRequest
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.service.embedding.EmbeddingService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime

class GithubReadmeEmbeddingProcessorTest : FunSpec({

    val embeddingService = mockk<EmbeddingService>()
    val processor = GithubReadmeEmbeddingProcessor(embeddingService)

    beforeTest { clearAllMocks() }

    fun createDto(
        id: Long = 1L,
        description: String? = "AI-powered CLI tool",
        readmeSummary: String? = "An AI-powered CLI tool that ...",
        topics: List<String> = listOf("kotlin", "ai"),
        ownerName: String = "owner",
    ) = GithubRepositoryDto(
        id = id, repoName = "repo", fullName = "$ownerName/repo",
        description = description, htmlUrl = "https://github.com/$ownerName/repo",
        starCount = 1000L, forkCount = 10L, primaryLanguage = "Kotlin",
        ownerName = ownerName, ownerAvatarUrl = null, topics = topics,
        pushedAt = LocalDateTime.now(), fetchedAt = LocalDateTime.now(),
        weeklyStarDelta = 0L, readmeSummary = readmeSummary,
    )

    fun successResult(vector: List<Float> = listOf(0.1f, 0.2f, 0.3f)) =
        EmbeddingService.EmbeddingResult(
            request = EmbeddingRequest("content", emptyList(), ""),
            vector = vector,
            success = true,
        )

    test("description과 readmeSummary로 임베딩 벡터를 생성하고 결과를 반환한다") {
        val dto = createDto()
        every { embeddingService.generateEmbeddingBatch(any(), any()) } returns listOf(successResult())

        val result = processor.process(dto)

        result.shouldNotBeNull()
        result.id shouldBe 1L
        result.fullName shouldBe "owner/repo"
        result.embeddingVector shouldBe floatArrayOf(0.1f, 0.2f, 0.3f)
    }

    test("description과 readmeSummary가 모두 null이면 임베딩을 호출하지 않고 null을 반환한다") {
        val dto = createDto(description = null, readmeSummary = null)

        val result = processor.process(dto)

        result.shouldBeNull()
        verify(exactly = 0) { embeddingService.generateEmbeddingBatch(any(), any()) }
    }

    test("description만 있어도 임베딩을 생성한다") {
        val dto = createDto(description = "A great tool", readmeSummary = null)
        every { embeddingService.generateEmbeddingBatch(any(), any()) } returns listOf(successResult())

        val result = processor.process(dto)

        result.shouldNotBeNull()
    }

    test("readmeSummary만 있어도 임베딩을 생성한다") {
        val dto = createDto(description = null, readmeSummary = "Summary content")
        every { embeddingService.generateEmbeddingBatch(any(), any()) } returns listOf(successResult())

        val result = processor.process(dto)

        result.shouldNotBeNull()
    }

    test("임베딩 생성 실패 시 null을 반환한다") {
        val dto = createDto()
        every { embeddingService.generateEmbeddingBatch(any(), any()) } returns listOf(
            EmbeddingService.EmbeddingResult(
                request = EmbeddingRequest("content", emptyList(), ""),
                vector = emptyList(), success = false, error = "API error"
            )
        )

        val result = processor.process(dto)

        result.shouldBeNull()
    }

    test("EmbeddingRequest에 topics와 ownerName이 포함된다") {
        val dto = createDto(topics = listOf("kotlin", "spring"), ownerName = "testowner")
        val capturedRequests = slot<List<EmbeddingRequest>>()
        every { embeddingService.generateEmbeddingBatch(capture(capturedRequests), any()) } returns listOf(successResult())

        processor.process(dto)

        capturedRequests.captured[0].categories shouldBe listOf("kotlin", "spring")
        capturedRequests.captured[0].companyName shouldBe "testowner"
    }
})
