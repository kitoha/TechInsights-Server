package com.techinsights.domain.service.post

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.repository.post.PostViewRepository
import com.techinsights.domain.service.company.CompanyViewCountUpdater
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import java.time.LocalDate
import java.time.LocalDateTime

class PostViewServiceTest : FunSpec({
  val postViewRepository = mockk<PostViewRepository>()
  val postRepository = mockk<PostRepository>()
  val viewCountUpdater = mockk<ViewCountUpdater>()
  val companyViewCountUpdater = mockk<CompanyViewCountUpdater>()
  val postViewService = PostViewService(
    postViewRepository,
    postRepository,
    viewCountUpdater,
    companyViewCountUpdater
  )

  val companyId = Tsid.encode(1)
  val postId = Tsid.encode(1)

  val sampleCompanyDto = CompanyDto(
    id = companyId,
    name = "Test Company",
    blogUrl = "http://testcompany.com/blog",
    logoImageName = "logo.png",
    rssSupported = true,
    totalViewCount = 100,
    postCount = 10
  )

  val samplePostDto = PostDto(
    id = postId,
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
    clearMocks(postViewRepository, postRepository, viewCountUpdater, companyViewCountUpdater)
  }

  test("조회 기록 - 첫 방문자") {
    val userOrIp = "127.0.0.1"
    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    val today = LocalDate.now()

    every {
      postViewRepository.existsByPostIdAndUserOrIpAndViewedDate(any(), userOrIp, today)
    } returns false
    every { postRepository.getCompanyIdByPostId(postId) } returns companyId
    every { postViewRepository.save(any()) } returns mockk()
    every { viewCountUpdater.incrementViewCount(postId) } just Runs
    every { companyViewCountUpdater.incrementTotalViewCount(companyId) } just Runs

    postViewService.recordView(postId, userOrIp, userAgent)

    verify(exactly = 1) {
      postViewRepository.existsByPostIdAndUserOrIpAndViewedDate(any(), userOrIp, today)
    }
    verify(exactly = 1) { postRepository.getCompanyIdByPostId(postId) }
    verify(exactly = 1) { postViewRepository.save(any()) }
    verify(exactly = 1) { viewCountUpdater.incrementViewCount(postId) }
    verify(exactly = 1) { companyViewCountUpdater.incrementTotalViewCount(companyId) }
  }

  test("조회 기록 - 이미 오늘 방문한 사용자") {
    val userOrIp = "127.0.0.1"
    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    val today = LocalDate.now()

    every {
      postViewRepository.existsByPostIdAndUserOrIpAndViewedDate(any(), userOrIp, today)
    } returns true

    postViewService.recordView(postId, userOrIp, userAgent)

    verify(exactly = 1) {
      postViewRepository.existsByPostIdAndUserOrIpAndViewedDate(any(), userOrIp, today)
    }
    verify(exactly = 0) { postRepository.getCompanyIdByPostId(any()) }
    verify(exactly = 0) { postViewRepository.save(any()) }
    verify(exactly = 0) { viewCountUpdater.incrementViewCount(any()) }
    verify(exactly = 0) { companyViewCountUpdater.incrementTotalViewCount(any()) }
  }

  test("조회 기록 - 다른 IP에서 방문") {
    val userOrIp1 = "127.0.0.1"
    val userOrIp2 = "192.168.0.1"
    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    val today = LocalDate.now()

    every {
      postViewRepository.existsByPostIdAndUserOrIpAndViewedDate(any(), userOrIp1, today)
    } returns false
    every {
      postViewRepository.existsByPostIdAndUserOrIpAndViewedDate(any(), userOrIp2, today)
    } returns false
    every { postRepository.getCompanyIdByPostId(postId) } returns companyId
    every { postViewRepository.save(any()) } returns mockk()
    every { viewCountUpdater.incrementViewCount(postId) } just Runs
    every { companyViewCountUpdater.incrementTotalViewCount(companyId) } just Runs

    postViewService.recordView(postId, userOrIp1, userAgent)
    postViewService.recordView(postId, userOrIp2, userAgent)

    verify(exactly = 2) { postRepository.getCompanyIdByPostId(postId) }
    verify(exactly = 2) { postViewRepository.save(any()) }
    verify(exactly = 2) { viewCountUpdater.incrementViewCount(postId) }
    verify(exactly = 2) { companyViewCountUpdater.incrementTotalViewCount(companyId) }
  }
})
