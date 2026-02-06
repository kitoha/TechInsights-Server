package com.techinsights.api.controller.post

import com.techinsights.api.props.AidProperties
import com.techinsights.api.service.post.PostLikeService
import com.techinsights.api.util.auth.RequesterResolver
import com.techinsights.domain.dto.auth.Requester
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
    private val aidProperties = mockk<AidProperties>(relaxed = true)

    init {
        beforeTest {
            clearAllMocks()

            val controller = PostLikeController(postLikeService)
            val requesterResolver = RequesterResolver(aidProperties)
            
            mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(requesterResolver)
                .build()
        }

        test("POST /api/v1/posts/{postId}/like - Anonymous User - New Like") {
            val postId = Tsid.generate()

            every { postLikeService.toggleLike(postId, any<Requester.Anonymous>()) } returns true

            mockMvc.post("/api/v1/posts/$postId/like") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.liked") { value(true) }
            }

            verify(exactly = 1) { postLikeService.toggleLike(postId, any<Requester.Anonymous>()) }
        }

        test("POST /api/v1/posts/{postId}/like - Anonymous User - Unlike") {
            val postId = Tsid.generate()

            every { postLikeService.toggleLike(postId, any<Requester.Anonymous>()) } returns false

            mockMvc.post("/api/v1/posts/$postId/like") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.liked") { value(false) }
            }

            verify(exactly = 1) { postLikeService.toggleLike(postId, any<Requester.Anonymous>()) }
        }

        test("POST /api/v1/posts/{postId}/like - Authenticated User - New Like") {
            val postId = Tsid.generate()

            every { postLikeService.toggleLike(postId, any<Requester.Authenticated>()) } returns true

            mockMvc.post("/api/v1/posts/$postId/like") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.liked") { value(true) }
            }

            verify(exactly = 1) { postLikeService.toggleLike(postId, any<Requester.Authenticated>()) }
        }

        test("POST /api/v1/posts/{postId}/like - Authenticated User - Unlike") {
            val postId = Tsid.generate()

            every { postLikeService.toggleLike(postId, any<Requester.Authenticated>()) } returns false

            mockMvc.post("/api/v1/posts/$postId/like") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.liked") { value(false) }
            }

            verify(exactly = 1) { postLikeService.toggleLike(postId, any<Requester.Authenticated>()) }
        }
    }
}
