package com.techinsights.api.controller.post

import com.techinsights.api.service.post.PostLikeService
import com.techinsights.api.util.ClientIpExtractor
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class PostLikeControllerTest : FunSpec() {

    private lateinit var mockMvc: MockMvc
    private val postLikeService = mockk<PostLikeService>()
    private val clientIpExtractor = mockk<ClientIpExtractor>()

    init {
        beforeTest {
            clearAllMocks()

            val controller = PostLikeController(postLikeService, clientIpExtractor)
            mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        }

        test("POST /api/v1/posts/{postId}/like - Anonymous User - New Like") {
            val postId = Tsid.generate()
            val clientIp = "127.0.0.1"

            every { clientIpExtractor.extract(any()) } returns clientIp
            every { postLikeService.toggleLike(postId, null, clientIp) } returns true

            mockMvc.post("/api/v1/posts/$postId/like") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.liked") { value(true) }
            }

            verify(exactly = 1) { clientIpExtractor.extract(any()) }
            verify(exactly = 1) { postLikeService.toggleLike(postId, null, clientIp) }
        }

        test("POST /api/v1/posts/{postId}/like - Anonymous User - Unlike") {
            val postId = Tsid.generate()
            val clientIp = "192.168.0.1"

            every { clientIpExtractor.extract(any()) } returns clientIp
            every { postLikeService.toggleLike(postId, null, clientIp) } returns false

            mockMvc.post("/api/v1/posts/$postId/like") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.liked") { value(false) }
            }

            verify(exactly = 1) { postLikeService.toggleLike(postId, null, clientIp) }
        }

        test("POST /api/v1/posts/{postId}/like - Authenticated User - New Like") {
            val postId = Tsid.generate()
            val userId = 100L
            val clientIp = "10.0.0.1"

            every { clientIpExtractor.extract(any()) } returns clientIp
            every { postLikeService.toggleLike(postId, userId, clientIp) } returns true

            mockMvc.post("/api/v1/posts/$postId/like") {
                accept = MediaType.APPLICATION_JSON
                header("X-User-Id", userId.toString())
            }.andExpect {
                status { isOk() }
                jsonPath("$.liked") { value(true) }
            }

            verify(exactly = 1) { postLikeService.toggleLike(postId, userId, clientIp) }
        }

        test("POST /api/v1/posts/{postId}/like - Authenticated User - Unlike") {
            val postId = Tsid.generate()
            val userId = 200L
            val clientIp = "172.16.0.1"

            every { clientIpExtractor.extract(any()) } returns clientIp
            every { postLikeService.toggleLike(postId, userId, clientIp) } returns false

            mockMvc.post("/api/v1/posts/$postId/like") {
                accept = MediaType.APPLICATION_JSON
                header("X-User-Id", userId.toString())
            }.andExpect {
                status { isOk() }
                jsonPath("$.liked") { value(false) }
            }

            verify(exactly = 1) { postLikeService.toggleLike(postId, userId, clientIp) }
        }
    }
}
