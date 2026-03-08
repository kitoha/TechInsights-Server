package com.techinsights.api.github

import com.techinsights.api.aid.AidProperties
import com.techinsights.api.auth.RequesterResolver
import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.service.github.GithubBookmarkService
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class GithubBookmarkControllerTest : FunSpec() {

    private lateinit var mockMvc: MockMvc
    private val githubBookmarkService = mockk<GithubBookmarkService>()
    private val aidProperties = mockk<AidProperties>(relaxed = true)

    init {
        beforeTest {
            clearAllMocks()

            val controller = GithubBookmarkController(githubBookmarkService)
            val requesterResolver = RequesterResolver(aidProperties)

            mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(requesterResolver)
                .build()
        }

        test("POST /api/v1/github/{repoId}/bookmark - 북마크 추가 (bookmarked: true)") {
            val repoId = 100L
            every { githubBookmarkService.toggleBookmark(repoId, any<Requester>()) } returns true

            mockMvc.post("/api/v1/github/$repoId/bookmark") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.bookmarked") { value(true) }
            }

            verify(exactly = 1) { githubBookmarkService.toggleBookmark(repoId, any<Requester>()) }
        }

        test("POST /api/v1/github/{repoId}/bookmark - 북마크 취소 (bookmarked: false)") {
            val repoId = 100L
            every { githubBookmarkService.toggleBookmark(repoId, any<Requester>()) } returns false

            mockMvc.post("/api/v1/github/$repoId/bookmark") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.bookmarked") { value(false) }
            }

            verify(exactly = 1) { githubBookmarkService.toggleBookmark(repoId, any<Requester>()) }
        }
    }
}
