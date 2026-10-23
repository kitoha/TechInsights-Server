package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.AnonymousUserReadHistory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.domain.PageRequest

class AnonymousUserReadHistoryRepositoryImplTest : FunSpec({
  val jpaRepository = mockk<AnonymousUserReadHistoryJpaRepository>()
  val repository = AnonymousUserReadHistoryRepositoryImpl(jpaRepository)

  test("읽기 이력은 전달받은 식별자(aid)로 저장한다") {
    val saved = slot<AnonymousUserReadHistory>()
    every { jpaRepository.save(capture(saved)) } answers { saved.captured }

    repository.trackAnonymousPostRead("aid-1", "0000000000001")

    saved.captured.anonymousId shouldBe "aid-1"
  }

  test("최근 읽기 이력은 Pageable 크기만큼 조회한다") {
    val pageable = PageRequest.of(0, 10)
    every { jpaRepository.findByAnonymousIdOrderByReadAtDesc("aid-1", pageable) } returns
      listOf(AnonymousUserReadHistory(anonymousId = "aid-1", postId = 1L))

    val result = repository.getRecentReadHistory("aid-1", pageable)

    result.size shouldBe 1
    result[0].anonymousId shouldBe "aid-1"
    verify(exactly = 1) { jpaRepository.findByAnonymousIdOrderByReadAtDesc("aid-1", pageable) }
  }
})
