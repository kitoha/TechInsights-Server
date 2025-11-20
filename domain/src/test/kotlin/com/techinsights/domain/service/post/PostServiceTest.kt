package com.techinsights.domain.service.post

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.enums.PostSortType
import com.techinsights.domain.exception.PostNotFoundException
import com.techinsights.domain.repository.post.PostRepository
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
  val postService = PostService(postRepository)

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

  test("게시글 목록 조회 - RECENT 정렬") {
    val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publishedAt"))
    val page = PageImpl(listOf(samplePostDto))

    every {
      postRepository.getAllPosts(pageable, null)
    } returns page

    val result = postService.getPosts(0, 10, PostSortType.RECENT, Category.All, null)

    result.content shouldHaveSize 1
    result.content[0] shouldBe samplePostDto
    verify(exactly = 1) { postRepository.getAllPosts(pageable, null) }
  }

  test("게시글 목록 조회 - POPULAR 정렬") {
    val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "viewCount"))
    val page = PageImpl(listOf(samplePostDto))

    every {
      postRepository.getPostsByCategory(pageable, Category.BackEnd, null)
    } returns page

    val result = postService.getPosts(0, 10, PostSortType.POPULAR, Category.BackEnd, null)

    result.content shouldHaveSize 1
    result.content[0] shouldBe samplePostDto
    verify(exactly = 1) { postRepository.getPostsByCategory(pageable, Category.BackEnd, null) }
  }

  test("게시글 목록 조회 - 특정 회사 필터링") {
    val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publishedAt"))
    val page = PageImpl(listOf(samplePostDto))

    every {
      postRepository.getAllPosts(pageable, "1")
    } returns page

    val result = postService.getPosts(0, 10, PostSortType.RECENT, Category.All, "1")

    result.content shouldHaveSize 1
    result.content[0].company.id shouldBe "1"
    verify(exactly = 1) { postRepository.getAllPosts(pageable, "1") }
  }

  test("게시글 상세 조회 - 성공") {
    every { postRepository.getPostById("1") } returns samplePostDto

    val result = postService.getPostById("1")

    result shouldBe samplePostDto
    verify(exactly = 1) { postRepository.getPostById("1") }
  }

  test("게시글 상세 조회 - 존재하지 않는 게시글") {
    every { postRepository.getPostById("999") } throws PostNotFoundException("Post not found")

    shouldThrow<PostNotFoundException> {
      postService.getPostById("999")
    }

    verify(exactly = 1) { postRepository.getPostById("999") }
  }
})
