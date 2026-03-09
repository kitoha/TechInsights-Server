package com.techinsights.api.github

import com.techinsights.api.aid.AidProperties
import com.techinsights.api.auth.CustomUserDetails
import com.techinsights.api.auth.RequesterResolver
import com.techinsights.api.exception.GlobalExceptionHandler
import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.dto.github.GithubRepositoryDto
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.exception.UnauthorizedException
import com.techinsights.domain.service.github.GithubBookmarkService
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

class GithubBookmarkControllerTest : FunSpec() {

    private lateinit var mockMvc: MockMvc
    private val githubBookmarkService = mockk<GithubBookmarkService>()
    private val aidProperties = mockk<AidProperties>(relaxed = true)

    init {
        beforeTest {
            clearAllMocks()
            SecurityContextHolder.clearContext()

            val controller = GithubBookmarkController(githubBookmarkService)
            val requesterResolver = RequesterResolver(aidProperties)

            mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(requesterResolver)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
        }

        afterTest {
            SecurityContextHolder.clearContext()
        }

        test("POST /api/v1/github/{repoId}/bookmark - 인증된 사용자 - 북마크 추가 (bookmarked: true)") {
            val repoId = Tsid.encode(100L)
            val userDetails = CustomUserDetails(userId = 1L, email = "test@example.com", role = UserRole.USER)
            val auth = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
            SecurityContextHolder.getContext().authentication = auth

            every { githubBookmarkService.toggleBookmark(repoId, any<Requester.Authenticated>()) } returns true

            mockMvc.post("/api/v1/github/$repoId/bookmark") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.bookmarked") { value(true) }
            }

            verify(exactly = 1) { githubBookmarkService.toggleBookmark(repoId, any<Requester.Authenticated>()) }
        }

        test("POST /api/v1/github/{repoId}/bookmark - 인증된 사용자 - 북마크 취소 (bookmarked: false)") {
            val repoId = Tsid.encode(100L)
            val userDetails = CustomUserDetails(userId = 1L, email = "test@example.com", role = UserRole.USER)
            val auth = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
            SecurityContextHolder.getContext().authentication = auth

            every { githubBookmarkService.toggleBookmark(repoId, any<Requester.Authenticated>()) } returns false

            mockMvc.post("/api/v1/github/$repoId/bookmark") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.bookmarked") { value(false) }
            }

            verify(exactly = 1) { githubBookmarkService.toggleBookmark(repoId, any<Requester.Authenticated>()) }
        }

        test("POST /api/v1/github/{repoId}/bookmark - 익명 사용자 요청 시 401 반환") {
            val repoId = Tsid.encode(100L)
            // SecurityContext 비움 → RequesterResolver가 Requester.Anonymous 반환
            every { githubBookmarkService.toggleBookmark(repoId, any<Requester.Anonymous>()) } throws UnauthorizedException()

            mockMvc.post("/api/v1/github/$repoId/bookmark") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("UNAUTHORIZED") }
            }

            verify(exactly = 1) { githubBookmarkService.toggleBookmark(repoId, any<Requester.Anonymous>()) }
        }

        test("GET /api/v1/github/me/bookmarks - 인증된 사용자 - 북마크 목록 반환") {
            val userId = 1L
            val userDetails = CustomUserDetails(userId = userId, email = "test@example.com", role = UserRole.USER)
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)

            val repoId = 200L
            val repoDto = GithubRepositoryDto(
                id = repoId, repoName = "repo", fullName = "owner/repo",
                description = "desc", htmlUrl = "https://github.com/owner/repo",
                starCount = 100, forkCount = 10, primaryLanguage = "Kotlin",
                ownerName = "owner", ownerAvatarUrl = null, topics = emptyList(),
                pushedAt = LocalDateTime.now(), fetchedAt = LocalDateTime.now(),
                weeklyStarDelta = 5, readmeSummary = null,
            )
            val page = PageImpl(listOf(repoDto), PageRequest.of(0, 20), 1)

            every { githubBookmarkService.getMyBookmarks(userId, any()) } returns page

            mockMvc.get("/api/v1/github/me/bookmarks") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.content[0].id") { value(Tsid.encode(repoId)) }
                jsonPath("$.totalElements") { value(1) }
            }

            verify(exactly = 1) { githubBookmarkService.getMyBookmarks(userId, any()) }
        }

        test("GET /api/v1/github/me/bookmarks - 익명 사용자 - 401 반환") {
            mockMvc.get("/api/v1/github/me/bookmarks") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.errorCode") { value("UNAUTHORIZED") }
            }
        }
    }
}
