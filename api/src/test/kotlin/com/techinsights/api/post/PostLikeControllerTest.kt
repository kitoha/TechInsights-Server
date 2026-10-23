package com.techinsights.api.post

import com.techinsights.api.aid.AidInterceptor
import com.techinsights.api.aid.AidProperties
import com.techinsights.api.auth.CustomUserDetails
import com.techinsights.api.auth.RequesterResolver
import com.techinsights.domain.enums.UserRole
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.service.post.PostLikeService
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import jakarta.servlet.http.Cookie
import org.springframework.http.MediaType
import com.techinsights.domain.dto.post.LikeStatusResult
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class PostLikeControllerTest : FunSpec() {

    private lateinit var mockMvc: MockMvc
    private val postLikeService = mockk<PostLikeService>()
    private val aidProperties = mockk<AidProperties>(relaxed = true)

    init {
        beforeTest {
            clearAllMocks()
            SecurityContextHolder.clearContext()

            val controller = PostLikeController(postLikeService)
            val requesterResolver = RequesterResolver(aidProperties)
            
            mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(requesterResolver)
                .build()
        }

        afterTest {
            SecurityContextHolder.clearContext()
        }

        test("POST /api/v1/posts/{postId}/like - Anonymous User - New Like") {
            every { aidProperties.cookie } returns AidProperties.Cookie()
            val postId = Tsid.generate()

            every { postLikeService.toggleLike(postId, any<Requester.Anonymous>()) } returns true

            mockMvc.post("/api/v1/posts/$postId/like") {
                cookie(Cookie("aid", "aid-1"))
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.liked") { value(true) }
            }

            verify(exactly = 1) { postLikeService.toggleLike(postId, any<Requester.Anonymous>()) }
        }

        test("POST /api/v1/posts/{postId}/like - Anonymous User - Unlike") {
            every { aidProperties.cookie } returns AidProperties.Cookie()
            val postId = Tsid.generate()

            every { postLikeService.toggleLike(postId, any<Requester.Anonymous>()) } returns false

            mockMvc.post("/api/v1/posts/$postId/like") {
                cookie(Cookie("aid", "aid-1"))
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.liked") { value(false) }
            }

            verify(exactly = 1) { postLikeService.toggleLike(postId, any<Requester.Anonymous>()) }
        }

        test("POST /api/v1/posts/{postId}/like - 방금 발급된 aid(쿠키 없음)는 200 liked=true 를 주지만 기록하지 않는다") {
            val postId = Tsid.generate()
            every { aidProperties.cookie } returns AidProperties.Cookie()

            mockMvc.post("/api/v1/posts/$postId/like") {
                requestAttr(AidInterceptor.AID_ATTRIBUTE, "just-minted-aid")
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.liked") { value(true) }
            }

            verify(exactly = 0) { postLikeService.toggleLike(any(), any()) }
        }

        test("POST /api/v1/posts/{postId}/like - aid 도 쿠키도 없으면(봇 등) 기록하지 않는다") {
            val postId = Tsid.generate()
            every { aidProperties.cookie } returns AidProperties.Cookie()

            mockMvc.post("/api/v1/posts/$postId/like") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }

            verify(exactly = 0) { postLikeService.toggleLike(any(), any()) }
        }

        test("POST /api/v1/posts/{postId}/like - Authenticated User - New Like") {
            val userDetails = CustomUserDetails(userId = 1L, email = "test@example.com", role = UserRole.USER)
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
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

        test("GET /api/v1/posts/{postId}/like - Authenticated User - Liked") {
            val postId = Tsid.generate()

            every { postLikeService.getLikeStatus(postId, any<Requester.Authenticated>()) } returns LikeStatusResult(42L, true)

            mockMvc.get("/api/v1/posts/$postId/like") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.count") { value(42) }
                jsonPath("$.liked") { value(true) }
            }

            verify(exactly = 1) { postLikeService.getLikeStatus(postId, any<Requester.Authenticated>()) }
        }

        test("PUT /api/v1/posts/{postId}/like - 멱등 좋아요 설정") {
            val postId = Tsid.generate()
            every { postLikeService.like(postId, any<Requester.Anonymous>()) } just Runs

            mockMvc.put("/api/v1/posts/$postId/like") { accept = MediaType.APPLICATION_JSON }.andExpect {
                status { isOk() }
                jsonPath("$.liked") { value(true) }
            }

            verify(exactly = 1) { postLikeService.like(postId, any<Requester.Anonymous>()) }
            verify(exactly = 0) { postLikeService.toggleLike(any(), any()) }
        }

        test("DELETE /api/v1/posts/{postId}/like - 멱등 좋아요 해제") {
            val postId = Tsid.generate()
            every { postLikeService.unlike(postId, any<Requester.Anonymous>()) } just Runs

            mockMvc.delete("/api/v1/posts/$postId/like") { accept = MediaType.APPLICATION_JSON }.andExpect {
                status { isOk() }
                jsonPath("$.liked") { value(false) }
            }

            verify(exactly = 1) { postLikeService.unlike(postId, any<Requester.Anonymous>()) }
        }

        test("GET /api/v1/posts/{postId}/like - Anonymous User - Not Liked") {
            val postId = Tsid.generate()

            every { postLikeService.getLikeStatus(postId, any<Requester.Anonymous>()) } returns LikeStatusResult(10L, false)

            mockMvc.get("/api/v1/posts/$postId/like") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.count") { value(10) }
                jsonPath("$.liked") { value(false) }
            }

            verify(exactly = 1) { postLikeService.getLikeStatus(postId, any<Requester.Anonymous>()) }
        }

        test("POST /api/v1/posts/{postId}/like - Authenticated User - Unlike") {
            val userDetails = CustomUserDetails(userId = 1L, email = "test@example.com", role = UserRole.USER)
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
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
