package com.techinsights.api.github

import com.techinsights.api.exception.GlobalExceptionHandler
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.enums.CommunityStatus
import com.techinsights.domain.exception.GithubRepositoryNotFoundException
import com.techinsights.domain.service.github.GithubTrendingService
import com.techinsights.domain.utils.encode
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

class GithubCommunityControllerTest : FunSpec() {

    private lateinit var mockMvc: MockMvc
    private val githubTrendingService = mockk<GithubTrendingService>()

    init {
        beforeTest {
            clearAllMocks()

            val controller = GithubCommunityController(
                githubTrendingService = githubTrendingService
            )
            mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
        }

        test("GET /api/v1/github/{id}/community - community 데이터가 있는 레포 - 200 반환") {
            val dto = createMockDto(1L).copy(
                communityStatus = CommunityStatus.COMPLETED,
                communitySentiment = """{"positive":0.8,"neutral":0.1,"negative":0.1}""",
                communityInsights = """[{"title":"Popular","description":"High activity"}]""",
                communityHighlights = """{"hnPosts":[],"redditPosts":[]}""",
                communityMentionCount = 42,
            )
            every { githubTrendingService.getRepositoryById(1L) } returns dto

            mockMvc.get("/api/v1/github/${1L.encode()}/community") {
                accept = MediaType.APPLICATION_JSON
            }
                .andExpect { match(MockMvcResultMatchers.status().isOk()) }
                .andExpect { match(MockMvcResultMatchers.jsonPath("$.communityStatus").value("COMPLETED")) }
                .andExpect { match(MockMvcResultMatchers.jsonPath("$.communityMentionCount").value(42)) }
                .andExpect { match(MockMvcResultMatchers.jsonPath("$.communitySentiment").isNotEmpty) }
        }

        test("GET /api/v1/github/{id}/community - 존재하지 않는 레포 - 404 반환") {
            every { githubTrendingService.getRepositoryById(any()) } throws GithubRepositoryNotFoundException("not found")

            mockMvc.get("/api/v1/github/${1L.encode()}/community") {
                accept = MediaType.APPLICATION_JSON
            }
                .andExpect { match(MockMvcResultMatchers.status().isNotFound()) }
        }
    }

    private fun createMockDto(
        id: Long,
        language: String? = "Kotlin",
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
        readmeSummary = null,
    )
}
