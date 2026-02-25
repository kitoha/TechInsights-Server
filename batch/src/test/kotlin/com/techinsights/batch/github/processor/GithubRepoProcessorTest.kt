package com.techinsights.batch.github.processor

import com.techinsights.batch.github.dto.GithubSearchResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class GithubRepoProcessorTest : FunSpec({

    val processor = GithubRepoProcessor()

    test("GitHub API Item을 GithubRepoUpsertData로 올바르게 변환한다") {
        val item = createItem(
            fullName = "spring-projects/spring-boot",
            stargazersCount = 75_000L,
            forksCount = 42_000L,
            language = "Java",
            topics = listOf("spring", "java"),
            pushedAt = "2024-06-01T10:00:00Z",
        )

        val result = processor.process(item)!!

        result.fullName shouldBe "spring-projects/spring-boot"
        result.repoName shouldBe "spring-boot"
        result.starCount shouldBe 75_000L
        result.forkCount shouldBe 42_000L
        result.primaryLanguage shouldBe "Java"
        result.ownerName shouldBe "spring-projects"
        result.id.shouldNotBeNull()
    }

    test("topics 리스트는 쉼표로 연결된 문자열로 변환된다") {
        val item = createItem(topics = listOf("spring", "java", "boot"))

        val result = processor.process(item)!!

        result.topics shouldBe "spring,java,boot"
    }

    test("topics가 비어있으면 null로 변환된다") {
        val item = createItem(topics = emptyList())

        val result = processor.process(item)!!

        result.topics shouldBe null
    }

    test("description이 null이면 null을 유지한다") {
        val item = createItem(description = null)

        val result = processor.process(item)!!

        result.description shouldBe null
    }

    test("language가 null이면 null을 유지한다") {
        val item = createItem(language = null)

        val result = processor.process(item)!!

        result.primaryLanguage shouldBe null
    }

    test("pushed_at ISO 8601 문자열을 LocalDateTime으로 파싱한다") {
        val item = createItem(pushedAt = "2024-01-15T12:34:56Z")

        val result = processor.process(item)!!

        result.pushedAt shouldBe LocalDateTime.of(2024, 1, 15, 12, 34, 56)
    }

    test("fetchedAt은 현재 시각으로 설정된다") {
        val before = LocalDateTime.now()
        val item = createItem()
        val result = processor.process(item)!!
        val after = LocalDateTime.now()

        (result.fetchedAt >= before) shouldBe true
        (result.fetchedAt <= after) shouldBe true
    }
})

private fun createItem(
    name: String = "spring-boot",
    fullName: String = "spring-projects/spring-boot",
    description: String? = "Spring Boot description",
    htmlUrl: String = "https://github.com/spring-projects/spring-boot",
    stargazersCount: Long = 1000L,
    forksCount: Long = 500L,
    language: String? = "Kotlin",
    ownerLogin: String = "spring-projects",
    ownerAvatarUrl: String? = "https://avatars.githubusercontent.com/u/317776",
    topics: List<String> = listOf("spring"),
    pushedAt: String = "2024-06-01T10:00:00Z",
): GithubSearchResponse.Item = GithubSearchResponse.Item(
    id = 1L,
    name = name,
    fullName = fullName,
    description = description,
    htmlUrl = htmlUrl,
    stargazersCount = stargazersCount,
    forksCount = forksCount,
    language = language,
    owner = GithubSearchResponse.Owner(login = ownerLogin, avatarUrl = ownerAvatarUrl),
    topics = topics,
    pushedAt = pushedAt,
)
