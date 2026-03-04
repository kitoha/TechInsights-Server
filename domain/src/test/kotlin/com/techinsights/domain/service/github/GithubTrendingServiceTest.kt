package com.techinsights.domain.service.github

import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.enums.GithubSortType
import com.techinsights.domain.exception.GithubRepositoryNotFoundException
import com.techinsights.domain.repository.github.GithubRepositoryRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class GithubTrendingServiceTest : FunSpec({

    val githubRepositoryRepository = mockk<GithubRepositoryRepository>()
    val service = GithubTrendingService(githubRepositoryRepository)

    val sampleDto = GithubRepositoryDto(
        id = 1L,
        repoName = "spring-boot",
        fullName = "spring-projects/spring-boot",
        description = "Spring Boot",
        htmlUrl = "https://github.com/spring-projects/spring-boot",
        starCount = 75_000L,
        forkCount = 42_000L,
        primaryLanguage = "Java",
        ownerName = "spring-projects",
        ownerAvatarUrl = "https://avatars.githubusercontent.com/u/317776",
        topics = listOf("spring", "java"),
        pushedAt = LocalDateTime.now(),
        fetchedAt = LocalDateTime.now(),
        weeklyStarDelta = 200L,
        readmeSummary = null,
    )

    beforeTest { clearAllMocks() }

    test("STARS 정렬로 레포지토리 목록 조회") {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(sampleDto))

        every {
            githubRepositoryRepository.findRepositories(pageable, GithubSortType.STARS, null)
        } returns page

        val result = service.getRepositories(0, 20, GithubSortType.STARS, null)

        result.content shouldHaveSize 1
        result.content[0] shouldBe sampleDto
        verify(exactly = 1) { githubRepositoryRepository.findRepositories(pageable, GithubSortType.STARS, null) }
    }

    test("TRENDING 정렬로 레포지토리 목록 조회") {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(sampleDto))

        every {
            githubRepositoryRepository.findRepositories(pageable, GithubSortType.TRENDING, null)
        } returns page

        val result = service.getRepositories(0, 20, GithubSortType.TRENDING, null)

        result.content shouldHaveSize 1
        result.content[0].weeklyStarDelta shouldBe 200L
        verify(exactly = 1) { githubRepositoryRepository.findRepositories(pageable, GithubSortType.TRENDING, null) }
    }

    test("LATEST 정렬로 레포지토리 목록 조회") {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(sampleDto))

        every {
            githubRepositoryRepository.findRepositories(pageable, GithubSortType.LATEST, null)
        } returns page

        val result = service.getRepositories(0, 20, GithubSortType.LATEST, null)

        result.content shouldHaveSize 1
        verify(exactly = 1) { githubRepositoryRepository.findRepositories(pageable, GithubSortType.LATEST, null) }
    }

    test("언어 필터로 레포지토리 목록 조회") {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(sampleDto))

        every {
            githubRepositoryRepository.findRepositories(pageable, GithubSortType.STARS, "Java")
        } returns page

        val result = service.getRepositories(0, 20, GithubSortType.STARS, "Java")

        result.content shouldHaveSize 1
        result.content[0].primaryLanguage shouldBe "Java"
        verify(exactly = 1) { githubRepositoryRepository.findRepositories(pageable, GithubSortType.STARS, "Java") }
    }

    test("레포지토리 단건 조회 - 성공") {
        every { githubRepositoryRepository.findById(1L) } returns sampleDto

        val result = service.getRepositoryById(1L)

        result shouldBe sampleDto
        verify(exactly = 1) { githubRepositoryRepository.findById(1L) }
    }

    test("레포지토리 단건 조회 - 존재하지 않는 레포지토리") {
        every { githubRepositoryRepository.findById(999L) } returns null

        shouldThrow<GithubRepositoryNotFoundException> {
            service.getRepositoryById(999L)
        }

        verify(exactly = 1) { githubRepositoryRepository.findById(999L) }
    }
})
