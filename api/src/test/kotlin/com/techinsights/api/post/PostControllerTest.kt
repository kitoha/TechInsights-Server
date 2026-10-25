package com.techinsights.api.post

import com.techinsights.api.aid.AidInterceptor
import com.techinsights.api.aid.AidProperties
import com.techinsights.api.auth.CustomUserDetails
import com.techinsights.api.auth.RequesterResolver
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.service.post.PostService
import com.techinsights.domain.service.post.PostViewService
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import jakarta.servlet.http.Cookie
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class PostControllerTest : FunSpec() {

  private lateinit var mockMvc: MockMvc
  private val postService = mockk<PostService>()
  private val postViewService = mockk<PostViewService>()
  private val aidProperties = mockk<AidProperties>(relaxed = true)

  init {
    beforeTest {
      clearAllMocks()

      val controller = PostController(
        postService,
        postViewService
      )
      val requesterResolver = RequesterResolver(aidProperties)
      
      mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setCustomArgumentResolvers(requesterResolver)
        .build()
    }

    test("GET /api/v1/posts - should return paginated posts with default parameters") {
      // given
      val posts = listOf(createMockPostDto("1"), createMockPostDto("2"))
      val page = org.springframework.data.domain.PageImpl(posts)

      every {
        postService.getPosts(0, 10, any(), any(), null)
      } returns page

      // when & then
      mockMvc.get("/api/v1/posts") {
        accept = org.springframework.http.MediaType.APPLICATION_JSON
      }.andExpect {
        status { isOk() }
      }

      verify(exactly = 1) {
        postService.getPosts(0, 10, any(), any(), null)
      }
    }

    test("GET /api/v1/posts - should accept custom pagination parameters") {
      // given
      val posts = listOf(createMockPostDto("1"))
      val page = org.springframework.data.domain.PageImpl(posts)

      every {
        postService.getPosts(2, 20, any(), any(), null)
      } returns page

      // when & then
      mockMvc.get("/api/v1/posts") {
        param("page", "2")
        param("size", "20")
        accept = org.springframework.http.MediaType.APPLICATION_JSON
      }.andExpect {
        status { isOk() }
      }

      verify(exactly = 1) {
        postService.getPosts(2, 20, any(), any(), null)
      }
    }

    test("GET /api/v1/posts - should filter by category") {
      // given
      val posts = listOf(createMockPostDto("1"))
      val page = org.springframework.data.domain.PageImpl(posts)

      every {
        postService.getPosts(0, 10, any(), com.techinsights.domain.enums.Category.AI, null)
      } returns page

      // when & then
      mockMvc.get("/api/v1/posts") {
        param("category", "AI")
        accept = org.springframework.http.MediaType.APPLICATION_JSON
      }.andExpect {
        status { isOk() }
      }

      verify(exactly = 1) {
        postService.getPosts(0, 10, any(), com.techinsights.domain.enums.Category.AI, null)
      }
    }

    test("GET /api/v1/posts - should filter by companyId") {
      // given
      val companyId = "company123"
      val posts = listOf(createMockPostDto("1"))
      val page = org.springframework.data.domain.PageImpl(posts)

      every {
        postService.getPosts(0, 10, any(), any(), companyId)
      } returns page

      // when & then
      mockMvc.get("/api/v1/posts") {
        param("companyId", companyId)
        accept = org.springframework.http.MediaType.APPLICATION_JSON
      }.andExpect {
        status { isOk() }
      }

      verify(exactly = 1) {
        postService.getPosts(0, 10, any(), any(), companyId)
      }
    }

    test("GET /api/v1/posts - should sort by POPULAR") {
      // given
      val posts = listOf(createMockPostDto("1"))
      val page = org.springframework.data.domain.PageImpl(posts)

      every {
        postService.getPosts(0, 10, com.techinsights.domain.enums.PostSortType.POPULAR, any(), null)
      } returns page

      // when & then
      mockMvc.get("/api/v1/posts") {
        param("sort", "POPULAR")
        accept = org.springframework.http.MediaType.APPLICATION_JSON
      }.andExpect {
        status { isOk() }
      }

      verify(exactly = 1) {
        postService.getPosts(0, 10, com.techinsights.domain.enums.PostSortType.POPULAR, any(), null)
      }
    }

    test("GET /api/v1/posts/{postId} - should return post by id") {
      // given
      val postId = "post123"
      val postDto = createMockPostDto(postId)

      every { postService.getPostById(postId) } returns postDto

      // when & then
      mockMvc.get("/api/v1/posts/$postId") {
        accept = org.springframework.http.MediaType.APPLICATION_JSON
      }.andExpect {
        status { isOk() }
      }

      verify(exactly = 1) { postService.getPostById(postId) }
    }

    test("POST /api/v1/posts/{postId}/view - 조회수 증가 성공") {
      every { aidProperties.cookie } returns AidProperties.Cookie()
      // given
      val postId = Tsid.encode(1L)
      val clientIp = "127.0.0.1"
      val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"


      every { postViewService.recordView(postId, any(), userAgent) } just Runs
      every { postViewService.trackAnonymousPostRead(any(), postId) } just Runs

      // when & then
      mockMvc.post("/api/v1/posts/$postId/view") {
        cookie(Cookie("aid", "aid-1"))
        header("User-Agent", userAgent)
      }.andExpect {
        status { isOk() }
      }


      verify(exactly = 1) { postViewService.recordView(postId, any(), userAgent) }
      verify(exactly = 1) { postViewService.trackAnonymousPostRead(any(), postId) }
    }

    test("POST /api/v1/posts/{postId}/view - 익명 사용자는 IP가 아닌 aid 쿠키로 조회/읽기 이력을 기록한다") {
      // given
      val postId = Tsid.encode(1L)
      every { aidProperties.cookie } returns AidProperties.Cookie()
      every { postViewService.recordView(postId, "aid-123", any()) } just Runs
      every { postViewService.trackAnonymousPostRead("aid-123", postId) } just Runs

      // when & then
      mockMvc.post("/api/v1/posts/$postId/view") {
        cookie(Cookie("aid", "aid-123"))
        header("X-Real-IP", "203.0.113.7")
      }.andExpect {
        status { isOk() }
      }

      verify(exactly = 1) { postViewService.recordView(postId, "aid-123", any()) }
      verify(exactly = 1) { postViewService.trackAnonymousPostRead("aid-123", postId) }
    }

    test("POST /api/v1/posts/{postId}/view - aid 쿠키 없이 방금 발급된 aid 로는 200 이지만 아무것도 기록하지 않는다") {
      // given
      val postId = Tsid.encode(1L)
      every { aidProperties.cookie } returns AidProperties.Cookie()

      // when & then
      mockMvc.post("/api/v1/posts/$postId/view") {
        requestAttr(AidInterceptor.AID_ATTRIBUTE, "just-minted-aid")
      }.andExpect {
        status { isOk() }
      }

      verify(exactly = 0) { postViewService.recordView(any(), any(), any()) }
      verify(exactly = 0) { postViewService.trackAnonymousPostRead(any(), any()) }
    }

    test("POST /api/v1/posts/{postId}/view - aid 도 쿠키도 없으면(봇 등) 200 이지만 아무것도 기록하지 않는다") {
      // given
      val postId = Tsid.encode(1L)
      every { aidProperties.cookie } returns AidProperties.Cookie()

      // when & then
      mockMvc.post("/api/v1/posts/$postId/view").andExpect {
        status { isOk() }
      }

      verify(exactly = 0) { postViewService.recordView(any(), any(), any()) }
      verify(exactly = 0) { postViewService.trackAnonymousPostRead(any(), any()) }
    }

    test("POST /api/v1/posts/{postId}/view - 로그인 사용자는 사용자 ID로 읽기 이력을 기록하고 익명 이력은 기록하지 않는다") {
      // given
      val postId = Tsid.encode(1L)
      val details = CustomUserDetails(userId = 42L, email = "u@example.com", role = UserRole.USER)
      SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(details, null, details.authorities)
      every { postViewService.recordView(postId, "U_42", any()) } just Runs
      every { postViewService.trackUserPostRead(42L, postId) } just Runs

      try {
        // when & then
        mockMvc.post("/api/v1/posts/$postId/view").andExpect {
          status { isOk() }
        }
      } finally {
        SecurityContextHolder.clearContext()
      }

      verify(exactly = 1) { postViewService.trackUserPostRead(42L, postId) }
      verify(exactly = 0) { postViewService.trackAnonymousPostRead(any(), any()) }
    }

    test("POST /api/v1/posts/{postId}/view - User-Agent 없이도 성공") {
      every { aidProperties.cookie } returns AidProperties.Cookie()
      // given
      val postId = Tsid.encode(1L)
      val clientIp = "192.168.0.1"


      every { postViewService.recordView(postId, any(), null) } just Runs
      every { postViewService.trackAnonymousPostRead(any(), postId) } just Runs

      // when & then
      mockMvc.post("/api/v1/posts/$postId/view") {
        cookie(Cookie("aid", "aid-1"))
        // User-Agent 헤더 없음
      }.andExpect {
        status { isOk() }
      }


      verify(exactly = 1) { postViewService.recordView(postId, any(), null) }
      verify(exactly = 1) { postViewService.trackAnonymousPostRead(any(), postId) }
    }

    test("POST /api/v1/posts/{postId}/view - 다양한 User-Agent 처리") {
      every { aidProperties.cookie } returns AidProperties.Cookie()
      // given
      val postId = Tsid.encode(1L)
      val clientIp = "10.0.0.1"
      val mobileUserAgent =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15"


      every { postViewService.recordView(postId, any(), mobileUserAgent) } just Runs
      every { postViewService.trackAnonymousPostRead(any(), postId) } just Runs

      // when & then
      mockMvc.post("/api/v1/posts/$postId/view") {
        cookie(Cookie("aid", "aid-1"))
        header("User-Agent", mobileUserAgent)
      }.andExpect {
        status { isOk() }
      }


      verify(exactly = 1) { postViewService.recordView(postId, any(), mobileUserAgent) }
      verify(exactly = 1) { postViewService.trackAnonymousPostRead(any(), postId) }
    }
  }

  private fun createMockPostDto(id: String): com.techinsights.domain.dto.post.PostDto {
    val companyDto = com.techinsights.domain.dto.company.CompanyDto(
      id = "company1",
      name = "Test Company",
      blogUrl = "https://test.com/blog",
      logoImageName = "logo.png",
      rssSupported = true
    )
    
    return com.techinsights.domain.dto.post.PostDto(
      id = id,
      title = "Test Post",
      preview = "Preview",
      url = "https://test.com/post",
      content = "Test Content",
      publishedAt = java.time.LocalDateTime.now(),
      thumbnail = null,
      company = companyDto,
      viewCount = 100,
      categories = setOf(com.techinsights.domain.enums.Category.AI),
      isSummary = true,
      isEmbedding = true
    )
  }
}
