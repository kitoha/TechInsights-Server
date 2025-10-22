package com.techinsights.domain.service.post

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.enums.PostSortType
import com.techinsights.domain.exception.PostNotFoundException
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.repository.user.AnonymousUserReadHistoryRepository
import com.techinsights.domain.utils.Tsid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDateTime

class PostServiceTest : FunSpec({
  val postRepository = mockk<PostRepository>()
  val postViewService = mockk<PostViewService>()
  val anonymousUserReadHistoryRepository = mockk<AnonymousUserReadHistoryRepository>()
  val postService = PostService(postRepository, postViewService, anonymousUserReadHistoryRepository)

  val sampleCompanyDto = CompanyDto(
    id = "1",
    name = "Test Company",
    blogUrl = "http://testcompany.com/blog",
    logoImageName = "logo.png",
    rssSupported = true,
    totalViewCount = 100,
    postCount = 10
  )

  val samplePostDto = PostDto(
    id = "1",
    title = "Test Post",
    preview = "Test preview",
    url = "http://test.com/post1",
    content = "Test content",
    publishedAt = LocalDateTime.now(),
    thumbnail = "thumbnail.png",
    company = sampleCompanyDto,
    viewCount = 10,
    categories = setOf(Category.BackEnd),
    isSummary = true,
    isEmbedding = false
  )

  beforeTest {
    clearMocks(postViewService, anonymousUserReadHistoryRepository)
  }

  test("게시글 목록 조회 - RECENT 정렬") {
    val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publishedAt"))
    val page = PageImpl(listOf(samplePostDto))

    every {
      postRepository.getPosts(pageable, Category.All, null)
    } returns page

    val result = postService.getPosts(0, 10, PostSortType.RECENT, Category.All, null)

    result.content shouldHaveSize 1
    result.content[0] shouldBe samplePostDto
    verify(exactly = 1) { postRepository.getPosts(pageable, Category.All, null) }
  }

  test("게시글 목록 조회 - POPULAR 정렬") {
    val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "viewCount"))
    val page = PageImpl(listOf(samplePostDto))

    every {
      postRepository.getPosts(pageable, Category.BackEnd, null)
    } returns page

    val result = postService.getPosts(0, 10, PostSortType.POPULAR, Category.BackEnd, null)

    result.content shouldHaveSize 1
    result.content[0] shouldBe samplePostDto
    verify(exactly = 1) { postRepository.getPosts(pageable, Category.BackEnd, null) }
  }

  test("게시글 목록 조회 - 특정 회사 필터링") {
    val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publishedAt"))
    val page = PageImpl(listOf(samplePostDto))

    every {
      postRepository.getPosts(pageable, Category.All, "1")
    } returns page

    val result = postService.getPosts(0, 10, PostSortType.RECENT, Category.All, "1")

    result.content shouldHaveSize 1
    result.content[0].company.id shouldBe "1"
    verify(exactly = 1) { postRepository.getPosts(pageable, Category.All, "1") }
  }

  test("게시글 상세 조회 - 성공") {
    val clientIp = "127.0.0.1"

    every { postRepository.getPostById("1") } returns samplePostDto
    every { postViewService.recordView(samplePostDto, clientIp) } just Runs
    every {
      anonymousUserReadHistoryRepository.trackAnonymousPostRead(clientIp, "1")
    } just Runs

    val result = postService.getPostById("1", clientIp)

    result shouldBe samplePostDto
    verify(exactly = 1) { postRepository.getPostById("1") }
    verify(exactly = 1) { postViewService.recordView(samplePostDto, clientIp) }
    verify(exactly = 1) { anonymousUserReadHistoryRepository.trackAnonymousPostRead(clientIp, "1") }
  }

  test("게시글 상세 조회 - 존재하지 않는 게시글") {
    val clientIp = "127.0.0.1"

    every { postRepository.getPostById("999") } throws PostNotFoundException("Post not found")

    shouldThrow<PostNotFoundException> {
      postService.getPostById("999", clientIp)
    }

    verify(exactly = 1) { postRepository.getPostById("999") }
    verify(exactly = 0) { postViewService.recordView(any(), any()) }
    verify(exactly = 0) { anonymousUserReadHistoryRepository.trackAnonymousPostRead(any(), any()) }
  }

  test("게시글 상세 조회 - 조회수 증가 실패해도 게시글은 반환") {
    val clientIp = "127.0.0.1"
    val postId = Tsid.generate()

    every { postRepository.getPostById(postId) } returns samplePostDto
    every {
      postViewService.recordView(
        samplePostDto,
        clientIp
      )
    } throws RuntimeException("View count error")
    every {
      anonymousUserReadHistoryRepository.trackAnonymousPostRead(clientIp, postId)
    } just Runs

    val result = postService.getPostById(postId, clientIp)

    result shouldBe samplePostDto
    verify(exactly = 1) { postRepository.getPostById(postId) }
  }
})
