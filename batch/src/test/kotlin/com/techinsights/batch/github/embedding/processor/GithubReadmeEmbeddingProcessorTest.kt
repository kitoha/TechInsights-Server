package com.techinsights.batch.github.embedding.processor

import com.techinsights.domain.dto.github.GithubRepositoryDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import java.time.LocalDateTime

class GithubReadmeEmbeddingProcessorTest : FunSpec({

    // EmbeddingService 의존 없음 — 콘텐츠 빌드만 담당
    val processor = GithubReadmeEmbeddingProcessor()

    fun createDto(
        id: Long = 1L,
        description: String? = "AI-powered CLI tool",
        readmeSummary: String? = "An AI-powered CLI tool that helps developers.",
        topics: List<String> = listOf("kotlin", "ai"),
        ownerName: String = "owner",
    ) = GithubRepositoryDto(
        id = id, repoName = "repo", fullName = "$ownerName/repo",
        description = description, htmlUrl = "https://github.com/$ownerName/repo",
        starCount = 1000L, forkCount = 10L, primaryLanguage = "Kotlin",
        ownerName = ownerName, ownerAvatarUrl = null, topics = topics,
        pushedAt = LocalDateTime.now(), fetchedAt = LocalDateTime.now(),
        weeklyStarDelta = 0L, dailyStarDelta = 0L, readmeSummary = readmeSummary,
    )

    test("description과 readmeSummary로 GithubEmbeddingRequestDto를 반환한다") {
        val dto = createDto()

        val result = processor.process(dto)

        result.shouldNotBeNull()
        result.id shouldBe 1L
        result.fullName shouldBe "owner/repo"
        result.promptString.shouldNotBeBlank()
    }

    test("description과 readmeSummary가 모두 null이면 null을 반환한다") {
        val result = processor.process(createDto(description = null, readmeSummary = null))

        result.shouldBeNull()
    }

    test("description만 있어도 DTO를 반환한다") {
        val result = processor.process(createDto(description = "A great tool", readmeSummary = null))

        result.shouldNotBeNull()
    }

    test("readmeSummary만 있어도 DTO를 반환한다") {
        val result = processor.process(createDto(description = null, readmeSummary = "Summary content"))

        result.shouldNotBeNull()
    }

    test("promptString에 topics(categories)와 ownerName이 포함된다") {
        val dto = createDto(topics = listOf("kotlin", "spring"), ownerName = "testowner")

        val result = processor.process(dto)!!

        result.promptString shouldContain "kotlin"
        result.promptString shouldContain "spring"
        result.promptString shouldContain "testowner"
    }

    test("promptString에 description과 readmeSummary 내용이 모두 포함된다") {
        val dto = createDto(description = "Awesome repo", readmeSummary = "It does great things")

        val result = processor.process(dto)!!

        result.promptString shouldContain "Awesome repo"
        result.promptString shouldContain "It does great things"
    }
})
