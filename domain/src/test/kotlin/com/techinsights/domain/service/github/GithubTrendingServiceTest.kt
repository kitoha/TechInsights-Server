package com.techinsights.domain.service.github

import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.dto.github.GithubRepositoryCursor
import com.techinsights.domain.dto.github.GithubSummaryDto
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
        dailyStarDelta = 30L,
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

    test("DAILY_TRENDING 정렬로 레포지토리 목록 조회 시 dailyStarDelta 반환") {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(sampleDto))

        every {
            githubRepositoryRepository.findRepositories(pageable, GithubSortType.DAILY_TRENDING, null)
        } returns page

        val result = service.getRepositories(0, 20, GithubSortType.DAILY_TRENDING, null)

        result.content shouldHaveSize 1
        result.content[0].dailyStarDelta shouldBe 30L
        verify(exactly = 1) { githubRepositoryRepository.findRepositories(pageable, GithubSortType.DAILY_TRENDING, null) }
    }

    test("cursor 없이 첫 페이지를 커서 기반으로 조회한다") {
        every {
            githubRepositoryRepository.findRepositoriesByCursor(21, GithubSortType.STARS, null, null)
        } returns listOf(sampleDto)

        val result = service.getRepositoriesByCursor(null, 20, GithubSortType.STARS, null)

        result.content shouldHaveSize 1
        result.hasNext shouldBe false
        result.nextCursor shouldBe null
        verify(exactly = 1) {
            githubRepositoryRepository.findRepositoriesByCursor(21, GithubSortType.STARS, null, null)
        }
    }

    test("다음 페이지가 있으면 nextCursor를 생성한다") {
        val secondDto = sampleDto.copy(id = 2L, starCount = 70_000L)
        every {
            githubRepositoryRepository.findRepositoriesByCursor(2, GithubSortType.STARS, null, null)
        } returns listOf(sampleDto, secondDto)

        val result = service.getRepositoriesByCursor(null, 1, GithubSortType.STARS, null)

        result.content shouldHaveSize 1
        result.hasNext shouldBe true
        result.nextCursor shouldBe GithubRepositoryCursor.fromDto(sampleDto, GithubSortType.STARS).encode()
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
    test("레포지토리 요약 정보 조회") {
        val summary = GithubSummaryDto(100L, 50000L)
        every {
            githubRepositoryRepository.countAndSumStars(GithubSortType.STARS, "Java")
        } returns summary

        val result = service.getSummary(GithubSortType.STARS, "Java")

        result shouldBe summary
        verify(exactly = 1) { githubRepositoryRepository.countAndSumStars(GithubSortType.STARS, "Java") }
    }
})
