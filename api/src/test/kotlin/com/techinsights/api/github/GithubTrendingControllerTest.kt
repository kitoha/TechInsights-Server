package com.techinsights.api.github

import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.enums.GithubSortType
import com.techinsights.domain.service.github.GithubTrendingService
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

class GithubTrendingControllerTest : FunSpec() {

    private lateinit var mockMvc: MockMvc
    private val githubTrendingService = mockk<GithubTrendingService>()
    private val dispatcher = Dispatchers.IO

    init {
        beforeTest {
            clearAllMocks()

            val controller = GithubTrendingController(
                githubTrendingService,
                dispatcher,
            )
            mockMvc = MockMvcBuilders.standaloneSetup(controller).build()

            // 기본 stub — 각 테스트에서 덮어쓸 수 있음
            every { githubTrendingService.getRepositories(any(), any(), any(), any()) } returns PageImpl(emptyList())
        }

        test("GET /api/v1/github/trending - 기본 파라미터(page=0, size=20, sort=STARS)로 200 반환") {
            // given
            val page = PageImpl(listOf(createMockDto(1L), createMockDto(2L)), PageRequest.of(0, 20), 2L)
            every { githubTrendingService.getRepositories(any(), any(), any(), any()) } returns page

            // when & then
            mockMvc.get("/api/v1/github/trending") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }

            verify(exactly = 1) { githubTrendingService.getRepositories(0, 20, GithubSortType.STARS, null) }
        }

        test("GET /api/v1/github/trending - 커스텀 page·size 파라미터 서비스에 전달") {
            // given
            val page = PageImpl(listOf(createMockDto(3L)), PageRequest.of(1, 10), 11L)
            every { githubTrendingService.getRepositories(any(), any(), any(), any()) } returns page

            // when & then
            mockMvc.get("/api/v1/github/trending") {
                param("page", "1")
                param("size", "10")
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }

            verify(exactly = 1) { githubTrendingService.getRepositories(1, 10, GithubSortType.STARS, null) }
        }

        test("GET /api/v1/github/trending - language 파라미터 서비스에 전달") {
            // given
            val page = PageImpl(listOf(createMockDto(1L, language = "Kotlin")))
            every { githubTrendingService.getRepositories(any(), any(), any(), any()) } returns page

            // when & then
            mockMvc.get("/api/v1/github/trending") {
                param("language", "Kotlin")
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }

            verify(exactly = 1) { githubTrendingService.getRepositories(0, 20, GithubSortType.STARS, "Kotlin") }
        }

        test("GET /api/v1/github/trending - sort=TRENDING 파라미터 서비스에 전달") {
            // given
            val page = PageImpl(listOf(createMockDto(1L)))
            every { githubTrendingService.getRepositories(any(), any(), any(), any()) } returns page

            // when & then
            mockMvc.get("/api/v1/github/trending") {
                param("sort", "TRENDING")
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }

            verify(exactly = 1) { githubTrendingService.getRepositories(0, 20, GithubSortType.TRENDING, null) }
        }

        test("GET /api/v1/github/trending - sort=LATEST 파라미터 서비스에 전달") {
            // given
            val page = PageImpl(listOf(createMockDto(1L)))
            every { githubTrendingService.getRepositories(any(), any(), any(), any()) } returns page

            // when & then
            mockMvc.get("/api/v1/github/trending") {
                param("sort", "LATEST")
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }

            verify(exactly = 1) { githubTrendingService.getRepositories(0, 20, GithubSortType.LATEST, null) }
        }

        test("GET /api/v1/github/trending - language 없이 null 전달") {
            // given
            every { githubTrendingService.getRepositories(any(), any(), any(), any()) } returns PageImpl(emptyList())

            // when & then
            mockMvc.get("/api/v1/github/trending") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }

            verify(exactly = 1) { githubTrendingService.getRepositories(0, 20, GithubSortType.STARS, null) }
        }
    }

    private fun createMockDto(
        id: Long,
        language: String? = "Kotlin",
        readmeSummary: String? = "AI 요약 테스트 문장입니다.",
    ): GithubRepositoryDto = GithubRepositoryDto(
        id = id,
        repoName = "repo-$id",
        fullName = "octocat/repo-$id",
        description = "Test repo $id",
        htmlUrl = "https://github.com/octocat/repo-$id",
        starCount = 1000L * id,
        forkCount = 100L * id,
        primaryLanguage = language,
        ownerName = "octocat",
        ownerAvatarUrl = "https://avatars.githubusercontent.com/u/583231",
        topics = listOf("test", "kotlin"),
        pushedAt = LocalDateTime.of(2025, 1, 1, 0, 0),
        fetchedAt = LocalDateTime.of(2025, 2, 1, 0, 0),
        weeklyStarDelta = 50L * id,
        readmeSummary = readmeSummary,
    )
}
