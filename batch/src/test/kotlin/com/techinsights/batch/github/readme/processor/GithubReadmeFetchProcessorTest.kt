package com.techinsights.batch.github.readme.processor

import com.techinsights.domain.dto.github.GithubRepositoryDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.mockk.*
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.Base64

class GithubReadmeFetchProcessorTest : FunSpec({

    fun buildProcessor(responseMono: Mono<String>): GithubReadmeFetchProcessor {
        val uriSpec = mockk<WebClient.RequestHeadersUriSpec<*>>(relaxed = true)
        val responseSpec = mockk<WebClient.ResponseSpec>()

        val webClient = mockk<WebClient>()
        every { webClient.get() } returns uriSpec
        every { uriSpec.uri(any<String>()) } answers { uriSpec }
        every { uriSpec.retrieve() } returns responseSpec
        every { responseSpec.onStatus(any(), any()) } returns responseSpec
        every { responseSpec.bodyToMono(String::class.java) } returns responseMono

        return GithubReadmeFetchProcessor(webClient)
    }

    test("README를 성공적으로 가져오면 ArticleInput을 반환한다") {
        val readmeContent = "# My Project\n\nThis is a test project."
        val encoded = Base64.getEncoder().encodeToString(readmeContent.toByteArray())
        val jsonResponse = """{"content":"$encoded","encoding":"base64"}"""

        val result = buildProcessor(Mono.just(jsonResponse)).process(createDto("owner/repo1"))
            .shouldNotBeNull()

        result.id shouldBe "owner/repo1"
        result.title shouldBe "repo1"
        result.content shouldBe readmeContent
    }

    test("README 내용이 2000자를 초과하면 2000자로 잘린다") {
        val longContent = "A".repeat(5000)
        val encoded = Base64.getEncoder().encodeToString(longContent.toByteArray())
        val jsonResponse = """{"content":"$encoded","encoding":"base64"}"""

        val result = buildProcessor(Mono.just(jsonResponse)).process(createDto("owner/repo"))
            .shouldNotBeNull()

        result.content shouldHaveLength 2000
    }

    test("404 응답 시 null을 반환한다") {
        val error = Mono.error<String>(WebClientResponseException.create(404, "Not Found", org.springframework.http.HttpHeaders.EMPTY, ByteArray(0), null))
        val result = buildProcessor(error).process(createDto("owner/no-readme-repo"))

        result.shouldBeNull()
    }

    test("기타 예외 발생 시 null을 반환한다") {
        val error = Mono.error<String>(RuntimeException("Network error"))
        val result = buildProcessor(error).process(createDto("owner/repo"))

        result.shouldBeNull()
    }

    test("base64 줄바꿈이 포함된 인코딩도 올바르게 디코딩한다") {
        val readmeContent = "Hello World"
        val rawEncoded = Base64.getEncoder().encodeToString(readmeContent.toByteArray())
        val encodedWithNewlines = rawEncoded.chunked(10).joinToString("\n")
        val jsonSafeEncoded = encodedWithNewlines.replace("\n", "\\n")
        val jsonResponse = """{"content":"$jsonSafeEncoded","encoding":"base64"}"""

        val result = buildProcessor(Mono.just(jsonResponse)).process(createDto("owner/repo"))
            .shouldNotBeNull()

        result.content shouldBe readmeContent
    }

    test("bodyToMono가 empty를 반환하면 null을 반환한다") {
        val result = buildProcessor(Mono.empty()).process(createDto("owner/repo"))

        result.shouldBeNull()
    }
})

private fun createDto(fullName: String) = GithubRepositoryDto(
    id = 1L,
    repoName = fullName.substringAfter("/"),
    fullName = fullName,
    description = null,
    htmlUrl = "https://github.com/$fullName",
    starCount = 1000L,
    forkCount = 100L,
    primaryLanguage = "Kotlin",
    ownerName = fullName.substringBefore("/"),
    ownerAvatarUrl = null,
    topics = emptyList(),
    pushedAt = LocalDateTime.now(),
    fetchedAt = LocalDateTime.now(),
    weeklyStarDelta = 0L,
    readmeSummary = null,
)
