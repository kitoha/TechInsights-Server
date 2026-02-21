package com.techinsights.api.post

import com.techinsights.api.aid.AidProperties
import com.techinsights.api.auth.RequesterResolver
import com.techinsights.domain.service.post.PostService
import com.techinsights.domain.service.post.PostViewService
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class PostControllerTest : FunSpec() {

  private lateinit var mockMvc: MockMvc
  private val postService = mockk<PostService>()
  private val postViewService = mockk<PostViewService>()
  private val dispatcher = Dispatchers.IO
  private val aidProperties = mockk<AidProperties>(relaxed = true)

  init {
    beforeTest {
      clearAllMocks()

      val controller = PostController(
        postService,
        postViewService,
        dispatcher
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

      coEvery { 
        postService.getPosts(0, 10, any(), any(), null) 
      } returns page

      // when & then
      mockMvc.get("/api/v1/posts") {
        accept = org.springframework.http.MediaType.APPLICATION_JSON
      }.andExpect {
        status { isOk() }
      }

      coVerify(exactly = 1) { 
        postService.getPosts(0, 10, any(), any(), null) 
      }
    }

    test("GET /api/v1/posts - should accept custom pagination parameters") {
      // given
      val posts = listOf(createMockPostDto("1"))
      val page = org.springframework.data.domain.PageImpl(posts)

      coEvery { 
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

      coVerify(exactly = 1) { 
        postService.getPosts(2, 20, any(), any(), null) 
      }
    }

    test("GET /api/v1/posts - should filter by category") {
      // given
      val posts = listOf(createMockPostDto("1"))
      val page = org.springframework.data.domain.PageImpl(posts)

      coEvery { 
        postService.getPosts(0, 10, any(), com.techinsights.domain.enums.Category.AI, null) 
      } returns page

      // when & then
      mockMvc.get("/api/v1/posts") {
        param("category", "AI")
        accept = org.springframework.http.MediaType.APPLICATION_JSON
      }.andExpect {
        status { isOk() }
      }

      coVerify(exactly = 1) { 
        postService.getPosts(0, 10, any(), com.techinsights.domain.enums.Category.AI, null) 
      }
    }

    test("GET /api/v1/posts - should filter by companyId") {
      // given
      val companyId = "company123"
      val posts = listOf(createMockPostDto("1"))
      val page = org.springframework.data.domain.PageImpl(posts)

      coEvery { 
        postService.getPosts(0, 10, any(), any(), companyId) 
      } returns page

      // when & then
      mockMvc.get("/api/v1/posts") {
        param("companyId", companyId)
        accept = org.springframework.http.MediaType.APPLICATION_JSON
      }.andExpect {
        status { isOk() }
      }

      coVerify(exactly = 1) { 
        postService.getPosts(0, 10, any(), any(), companyId) 
      }
    }

    test("GET /api/v1/posts - should sort by POPULAR") {
      // given
      val posts = listOf(createMockPostDto("1"))
      val page = org.springframework.data.domain.PageImpl(posts)

      coEvery { 
        postService.getPosts(0, 10, com.techinsights.domain.enums.PostSortType.POPULAR, any(), null) 
      } returns page

      // when & then
      mockMvc.get("/api/v1/posts") {
        param("sort", "POPULAR")
        accept = org.springframework.http.MediaType.APPLICATION_JSON
      }.andExpect {
        status { isOk() }
      }

      coVerify(exactly = 1) { 
        postService.getPosts(0, 10, com.techinsights.domain.enums.PostSortType.POPULAR, any(), null) 
      }
    }

    test("GET /api/v1/posts/{postId} - should return post by id") {
      // given
      val postId = "post123"
      val postDto = createMockPostDto(postId)

      coEvery { postService.getPostById(postId) } returns postDto

      // when & then
      mockMvc.get("/api/v1/posts/$postId") {
        accept = org.springframework.http.MediaType.APPLICATION_JSON
      }.andExpect {
        status { isOk() }
      }

      coVerify(exactly = 1) { postService.getPostById(postId) }
    }

    test("POST /api/v1/posts/{postId}/view - 조회수 증가 성공") {
      // given
      val postId = Tsid.encode(1L)
      val clientIp = "127.0.0.1"
      val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"


      every { postViewService.recordView(postId, any(), userAgent) } just Runs
      every { postViewService.trackAnonymousPostRead(any(), postId) } just Runs

      // when & then
      mockMvc.post("/api/v1/posts/$postId/view") {
        header("User-Agent", userAgent)
      }.andExpect {
        status { isOk() }
      }


      verify(exactly = 1) { postViewService.recordView(postId, any(), userAgent) }
      verify(exactly = 1) { postViewService.trackAnonymousPostRead(any(), postId) }
    }

    test("POST /api/v1/posts/{postId}/view - User-Agent 없이도 성공") {
      // given
      val postId = Tsid.encode(1L)
      val clientIp = "192.168.0.1"


      every { postViewService.recordView(postId, any(), null) } just Runs
      every { postViewService.trackAnonymousPostRead(any(), postId) } just Runs

      // when & then
      mockMvc.post("/api/v1/posts/$postId/view") {
        // User-Agent 헤더 없음
      }.andExpect {
        status { isOk() }
      }


      verify(exactly = 1) { postViewService.recordView(postId, any(), null) }
      verify(exactly = 1) { postViewService.trackAnonymousPostRead(any(), postId) }
    }

    test("POST /api/v1/posts/{postId}/view - 다양한 User-Agent 처리") {
      // given
      val postId = Tsid.encode(1L)
      val clientIp = "10.0.0.1"
      val mobileUserAgent =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15"


      every { postViewService.recordView(postId, any(), mobileUserAgent) } just Runs
      every { postViewService.trackAnonymousPostRead(any(), postId) } just Runs

      // when & then
      mockMvc.post("/api/v1/posts/$postId/view") {
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
