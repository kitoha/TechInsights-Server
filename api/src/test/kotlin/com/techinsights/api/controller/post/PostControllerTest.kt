package com.techinsights.api.controller.post

import com.techinsights.api.util.ClientIpExtractor
import com.techinsights.domain.repository.user.AnonymousUserReadHistoryRepository
import com.techinsights.domain.service.post.PostService
import com.techinsights.domain.service.post.PostViewService
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class PostControllerTest : FunSpec() {

  private lateinit var mockMvc: MockMvc
  private val postService = mockk<PostService>()
  private val postViewService = mockk<PostViewService>()
  private val clientIpExtractor = mockk<ClientIpExtractor>()
  private val anonymousUserReadHistoryRepository = mockk<AnonymousUserReadHistoryRepository>()

  init {
    beforeTest {
      clearAllMocks()

      val controller = PostController(
        postService,
        postViewService,
        clientIpExtractor,
        anonymousUserReadHistoryRepository
      )
      mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    test("POST /api/v1/posts/{postId}/view - 조회수 증가 성공") {
      // given
      val postId = Tsid.encode(1L)
      val clientIp = "127.0.0.1"
      val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

      every { clientIpExtractor.extract(any()) } returns clientIp
      every { postViewService.recordView(postId, clientIp, userAgent) } just Runs
      every {
        anonymousUserReadHistoryRepository.trackAnonymousPostRead(
          clientIp,
          postId
        )
      } just Runs

      // when & then
      mockMvc.post("/api/v1/posts/$postId/view") {
        header("User-Agent", userAgent)
      }.andExpect {
        status { isOk() }
      }

      verify(exactly = 1) { clientIpExtractor.extract(any()) }
      verify(exactly = 1) { postViewService.recordView(postId, clientIp, userAgent) }
      verify(exactly = 1) {
        anonymousUserReadHistoryRepository.trackAnonymousPostRead(
          clientIp,
          postId
        )
      }
    }

    test("POST /api/v1/posts/{postId}/view - User-Agent 없이도 성공") {
      // given
      val postId = Tsid.encode(1L)
      val clientIp = "192.168.0.1"

      every { clientIpExtractor.extract(any()) } returns clientIp
      every { postViewService.recordView(postId, clientIp, null) } just Runs
      every {
        anonymousUserReadHistoryRepository.trackAnonymousPostRead(
          clientIp,
          postId
        )
      } just Runs

      // when & then
      mockMvc.post("/api/v1/posts/$postId/view") {
        // User-Agent 헤더 없음
      }.andExpect {
        status { isOk() }
      }

      verify(exactly = 1) { clientIpExtractor.extract(any()) }
      verify(exactly = 1) { postViewService.recordView(postId, clientIp, null) }
      verify(exactly = 1) {
        anonymousUserReadHistoryRepository.trackAnonymousPostRead(
          clientIp,
          postId
        )
      }
    }

    test("POST /api/v1/posts/{postId}/view - 다양한 User-Agent 처리") {
      // given
      val postId = Tsid.encode(1L)
      val clientIp = "10.0.0.1"
      val mobileUserAgent =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15"

      every { clientIpExtractor.extract(any()) } returns clientIp
      every { postViewService.recordView(postId, clientIp, mobileUserAgent) } just Runs
      every {
        anonymousUserReadHistoryRepository.trackAnonymousPostRead(
          clientIp,
          postId
        )
      } just Runs

      // when & then
      mockMvc.post("/api/v1/posts/$postId/view") {
        header("User-Agent", mobileUserAgent)
      }.andExpect {
        status { isOk() }
      }

      verify(exactly = 1) { clientIpExtractor.extract(any()) }
      verify(exactly = 1) { postViewService.recordView(postId, clientIp, mobileUserAgent) }
      verify(exactly = 1) {
        anonymousUserReadHistoryRepository.trackAnonymousPostRead(
          clientIp,
          postId
        )
      }
    }
  }
}
