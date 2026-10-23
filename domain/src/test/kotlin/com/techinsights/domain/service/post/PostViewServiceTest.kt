package com.techinsights.domain.service.post

import com.techinsights.domain.event.ViewCountIncrementEvent
import com.techinsights.domain.exception.PostNotFoundException
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.repository.post.PostViewRepository
import com.techinsights.domain.repository.user.AnonymousUserReadHistoryRepository
import com.techinsights.domain.repository.user.UserReadHistoryRepository
import com.techinsights.domain.utils.Tsid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate

class PostViewServiceTest : FunSpec({
  val postViewRepository = mockk<PostViewRepository>()
  val postRepository = mockk<PostRepository>()
  val anonymousUserReadHistoryRepository = mockk<AnonymousUserReadHistoryRepository>()
  val userReadHistoryRepository = mockk<UserReadHistoryRepository>()
  val applicationEventPublisher = mockk<ApplicationEventPublisher>()
  val postViewService = PostViewService(
    postViewRepository,
    postRepository,
    anonymousUserReadHistoryRepository,
    userReadHistoryRepository,
    applicationEventPublisher
  )

  val companyId = Tsid.encode(1)
  val postId = Tsid.encode(1)

  beforeTest {
    clearMocks(
      postViewRepository, postRepository, anonymousUserReadHistoryRepository, userReadHistoryRepository,
      applicationEventPublisher
    )
  }

  test("조회 기록 - 첫 방문자(aid)") {
    val userOrIp = "aid-1"
    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    val today = LocalDate.now()

    every { postRepository.existsById(1L) } returns true
    every { postViewRepository.insertIfAbsent(any()) } returns true
    every { postRepository.getCompanyIdByPostId(postId) } returns companyId
    every { applicationEventPublisher.publishEvent(any<ViewCountIncrementEvent>()) } just Runs

    postViewService.recordView(postId, userOrIp, userAgent)

    verify(exactly = 1) {
      postViewRepository.insertIfAbsent(
        match { it.postId == postId && it.userOrIp == userOrIp && it.viewedDate == today && it.userAgent == userAgent }
      )
    }
    verify(exactly = 1) { postRepository.getCompanyIdByPostId(postId) }
    verify(exactly = 1) {
      applicationEventPublisher.publishEvent(
        match<ViewCountIncrementEvent> { it.postId == postId && it.companyId == companyId }
      )
    }
  }

  test("조회 기록 - 이미 오늘 방문한 사용자") {
    val userOrIp = "aid-1"
    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

    every { postRepository.existsById(1L) } returns true
    every { postViewRepository.insertIfAbsent(any()) } returns false

    postViewService.recordView(postId, userOrIp, userAgent)

    verify(exactly = 1) { postViewRepository.insertIfAbsent(any()) }
    verify(exactly = 0) { postRepository.getCompanyIdByPostId(any()) }
    verify(exactly = 0) { applicationEventPublisher.publishEvent(any<ViewCountIncrementEvent>()) }
  }

  test("조회 기록 - 다른 식별자로 방문") {
    val userOrIp1 = "aid-1"
    val userOrIp2 = "aid-2"
    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

    every { postRepository.existsById(1L) } returns true
    every { postViewRepository.insertIfAbsent(any()) } returns true
    every { postRepository.getCompanyIdByPostId(postId) } returns companyId
    every { applicationEventPublisher.publishEvent(any<ViewCountIncrementEvent>()) } just Runs

    postViewService.recordView(postId, userOrIp1, userAgent)
    postViewService.recordView(postId, userOrIp2, userAgent)

    verify(exactly = 2) { postRepository.getCompanyIdByPostId(postId) }
    verify(exactly = 2) { postViewRepository.insertIfAbsent(any()) }
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

  test("회원 최근 본 글 추적") {
    every { postRepository.existsById(1L) } returns true
    every { userReadHistoryRepository.trackUserPostRead(7L, postId) } just Runs

    postViewService.trackUserPostRead(7L, postId)

    verify(exactly = 1) { userReadHistoryRepository.trackUserPostRead(7L, postId) }
    verify(exactly = 0) { anonymousUserReadHistoryRepository.trackAnonymousPostRead(any(), any()) }
  }

  context("존재하지 않거나 잘못된 게시글 ID") {
    val unknownPostId = Tsid.encode(999)

    test("조회 기록 - 존재하지 않는 게시글이면 PostNotFoundException이 발생하고 아무것도 기록하지 않는다") {
      every { postRepository.existsById(999L) } returns false

      shouldThrow<PostNotFoundException> { postViewService.recordView(unknownPostId, "aid-1", null) }

      verify(exactly = 0) { postViewRepository.insertIfAbsent(any()) }
    }

    test("조회 기록 - 형식이 잘못된 게시글 ID도 PostNotFoundException이 발생한다") {
      shouldThrow<PostNotFoundException> { postViewService.recordView("not-a-tsid!", "aid-1", null) }

      verify(exactly = 0) { postViewRepository.insertIfAbsent(any()) }
    }

    test("회원 읽기 이력 - 존재하지 않는 게시글이면 PostNotFoundException이 발생하고 기록하지 않는다") {
      every { postRepository.existsById(999L) } returns false

      shouldThrow<PostNotFoundException> { postViewService.trackUserPostRead(7L, unknownPostId) }

      verify(exactly = 0) { userReadHistoryRepository.trackUserPostRead(any(), any()) }
    }

    test("회원 읽기 이력 - 형식이 잘못된 게시글 ID도 PostNotFoundException이 발생하고 기록하지 않는다") {
      shouldThrow<PostNotFoundException> { postViewService.trackUserPostRead(7L, "not-a-tsid!") }

      verify(exactly = 0) { userReadHistoryRepository.trackUserPostRead(any(), any()) }
    }
  }
})
