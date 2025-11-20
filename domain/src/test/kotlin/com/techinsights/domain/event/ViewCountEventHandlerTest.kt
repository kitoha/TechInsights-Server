package com.techinsights.domain.event

import com.techinsights.domain.service.company.CompanyViewCountUpdater
import com.techinsights.domain.service.post.ViewCountUpdater
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.mockk.*

class ViewCountEventHandlerTest : FunSpec({
  val viewCountUpdater = mockk<ViewCountUpdater>()
  val companyViewCountUpdater = mockk<CompanyViewCountUpdater>()
  val eventHandler = ViewCountEventHandler(viewCountUpdater, companyViewCountUpdater)

  val postId = Tsid.encode(1L)
  val companyId = Tsid.encode(1L)

  beforeTest {
    clearMocks(viewCountUpdater, companyViewCountUpdater)
  }

  test("이벤트 처리 성공 - 조회수 증가") {
    // given
    val event = ViewCountIncrementEvent(postId, companyId)
    every { viewCountUpdater.incrementViewCount(postId) } just Runs
    every { companyViewCountUpdater.incrementTotalViewCount(companyId) } just Runs

    // when
    eventHandler.handleViewCountIncrement(event)

    // then
    verify(exactly = 1) { viewCountUpdater.incrementViewCount(postId) }
    verify(exactly = 1) { companyViewCountUpdater.incrementTotalViewCount(companyId) }
  }

  test("Post 조회수 업데이트 실패 시 - 예외 처리") {
    // given
    val event = ViewCountIncrementEvent(postId, companyId)
    every { viewCountUpdater.incrementViewCount(postId) } throws RuntimeException("DB Error")
    every { companyViewCountUpdater.incrementTotalViewCount(companyId) } just Runs

    // when & then - 예외가 발생해도 핸들러는 정상 종료
    eventHandler.handleViewCountIncrement(event)

    verify(exactly = 1) { viewCountUpdater.incrementViewCount(postId) }
    // Company 업데이트는 호출되지 않음 (Post 업데이트에서 예외 발생)
    verify(exactly = 0) { companyViewCountUpdater.incrementTotalViewCount(companyId) }
  }

  test("Company 조회수 업데이트 실패 시 - 예외 처리") {
    // given
    val event = ViewCountIncrementEvent(postId, companyId)
    every { viewCountUpdater.incrementViewCount(postId) } just Runs
    every { companyViewCountUpdater.incrementTotalViewCount(companyId) } throws RuntimeException("DB Error")

    // when & then - 예외가 발생해도 핸들러는 정상 종료
    eventHandler.handleViewCountIncrement(event)

    verify(exactly = 1) { viewCountUpdater.incrementViewCount(postId) }
    verify(exactly = 1) { companyViewCountUpdater.incrementTotalViewCount(companyId) }
  }

  test("여러 이벤트 처리 - 독립적 실행") {
    // given
    val event1 = ViewCountIncrementEvent(postId, companyId)
    val postId2 = Tsid.encode(2L)
    val companyId2 = Tsid.encode(2L)
    val event2 = ViewCountIncrementEvent(postId2, companyId2)

    every { viewCountUpdater.incrementViewCount(any()) } just Runs
    every { companyViewCountUpdater.incrementTotalViewCount(any()) } just Runs

    // when
    eventHandler.handleViewCountIncrement(event1)
    eventHandler.handleViewCountIncrement(event2)

    // then
    verify(exactly = 1) { viewCountUpdater.incrementViewCount(postId) }
    verify(exactly = 1) { viewCountUpdater.incrementViewCount(postId2) }
    verify(exactly = 1) { companyViewCountUpdater.incrementTotalViewCount(companyId) }
    verify(exactly = 1) { companyViewCountUpdater.incrementTotalViewCount(companyId2) }
  }
})
