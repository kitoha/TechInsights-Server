package com.techinsights.api.github

import com.techinsights.domain.dto.github.GithubRepositoryCursorPage
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.enums.GithubSortType
import com.techinsights.domain.service.github.GithubSemanticSearchService
import com.techinsights.domain.service.github.GithubTrendingService
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class GithubTrendingControllerTest : FunSpec() {

    private lateinit var mockMvc: MockMvc
    private val githubTrendingService = mockk<GithubTrendingService>()
    private val githubSemanticSearchService = mockk<GithubSemanticSearchService>(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()

    init {
        beforeTest {
            clearAllMocks()

            val controller = GithubTrendingController(
                githubTrendingService = githubTrendingService,
                githubSemanticSearchService = githubSemanticSearchService,
                ioDispatcher = dispatcher,
            )
            mockMvc = MockMvcBuilders.standaloneSetup(controller).build()

            every {
                githubTrendingService.getRepositoriesByCursor(any(), any(), any(), any())
            } returns GithubRepositoryCursorPage(
                content = emptyList(),
                size = 20,
                hasNext = false,
                nextCursor = null,
            )
        }

        test("GET /api/v1/github/trending - 기본 파라미터(cursor=null, size=20, sort=STARS)로 200 반환") {
            every {
                githubTrendingService.getRepositoriesByCursor(any(), any(), any(), any())
            } returns GithubRepositoryCursorPage(
                content = listOf(createMockDto(1L), createMockDto(2L)),
                size = 20,
                hasNext = false,
                nextCursor = null,
            )

            val asyncResult = mockMvc.get("/api/v1/github/trending") {
                accept = MediaType.APPLICATION_JSON
            }.andReturn()

            mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.size").value(20))
                .andExpect(MockMvcResultMatchers.jsonPath("$.hasNext").value(false))

            verify(exactly = 1) {
                githubTrendingService.getRepositoriesByCursor(null, 20, GithubSortType.STARS, null)
            }
        }

        test("GET /api/v1/github/trending - cursor, size, language, sort 파라미터를 서비스에 전달") {
            every {
                githubTrendingService.getRepositoriesByCursor(any(), any(), any(), any())
            } returns GithubRepositoryCursorPage(
                content = listOf(createMockDto(3L)),
                size = 10,
                hasNext = true,
                nextCursor = "next-cursor-token",
            )

            val asyncResult = mockMvc.get("/api/v1/github/trending") {
                param("cursor", "cursor-token")
                param("size", "10")
                param("sort", "TRENDING")
                param("language", "Kotlin")
                accept = MediaType.APPLICATION_JSON
            }.andReturn()

            mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.hasNext").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.nextCursor").value("next-cursor-token"))

            verify(exactly = 1) {
                githubTrendingService.getRepositoriesByCursor("cursor-token", 10, GithubSortType.TRENDING, "Kotlin")
            }
        }

        test("GET /api/v1/github/trending - dailyStarDelta가 포함된 DTO로 200 반환") {
            every {
                githubTrendingService.getRepositoriesByCursor(any(), any(), any(), any())
            } returns GithubRepositoryCursorPage(
                content = listOf(createMockDto(1L)),
                size = 20,
                hasNext = false,
                nextCursor = null,
            )

            val asyncResult = mockMvc.get("/api/v1/github/trending") {
                accept = MediaType.APPLICATION_JSON
            }.andReturn()

            mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].dailyStarDelta").value(10))
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
        dailyStarDelta = 10L * id,
        readmeSummary = readmeSummary,
    )
}
