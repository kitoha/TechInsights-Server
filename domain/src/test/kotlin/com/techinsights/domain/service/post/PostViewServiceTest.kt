package com.techinsights.domain.service.post

import com.techinsights.domain.event.ViewCountIncrementEvent
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.repository.post.PostViewRepository
import com.techinsights.domain.repository.user.AnonymousUserReadHistoryRepository
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate

class PostViewServiceTest : FunSpec({
  val postViewRepository = mockk<PostViewRepository>()
  val postRepository = mockk<PostRepository>()
  val anonymousUserReadHistoryRepository = mockk<AnonymousUserReadHistoryRepository>()
  val applicationEventPublisher = mockk<ApplicationEventPublisher>()
  val postViewService = PostViewService(
    postViewRepository,
    postRepository,
    anonymousUserReadHistoryRepository,
    applicationEventPublisher
  )

  val companyId = Tsid.encode(1)
  val postId = Tsid.encode(1)

  beforeTest {
    clearMocks(postViewRepository, postRepository, anonymousUserReadHistoryRepository, applicationEventPublisher)
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
    every { applicationEventPublisher.publishEvent(any<ViewCountIncrementEvent>()) } just Runs

    postViewService.recordView(postId, userOrIp, userAgent)

    verify(exactly = 1) {
      postViewRepository.existsByPostIdAndUserOrIpAndViewedDate(any(), userOrIp, today)
    }
    verify(exactly = 1) { postRepository.getCompanyIdByPostId(postId) }
    verify(exactly = 1) { postViewRepository.save(any()) }
    verify(exactly = 1) {
      applicationEventPublisher.publishEvent(
        match<ViewCountIncrementEvent> { it.postId == postId && it.companyId == companyId }
      )
    }
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
    verify(exactly = 0) { applicationEventPublisher.publishEvent(any<ViewCountIncrementEvent>()) }
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
    every { applicationEventPublisher.publishEvent(any<ViewCountIncrementEvent>()) } just Runs

    postViewService.recordView(postId, userOrIp1, userAgent)
    postViewService.recordView(postId, userOrIp2, userAgent)

    verify(exactly = 2) { postRepository.getCompanyIdByPostId(postId) }
    verify(exactly = 2) { postViewRepository.save(any()) }
    verify(exactly = 2) {
      applicationEventPublisher.publishEvent(
        match<ViewCountIncrementEvent> { it.postId == postId && it.companyId == companyId }
      )
    }
  }

  test("비회원 최근 본 글 추적") {
    every { anonymousUserReadHistoryRepository.trackAnonymousPostRead("anon-1", postId) } just Runs

    postViewService.trackAnonymousPostRead("anon-1", postId)

    verify(exactly = 1) { anonymousUserReadHistoryRepository.trackAnonymousPostRead("anon-1", postId) }
  }
})
