package com.techinsights.domain.repository.post

import com.techinsights.domain.dto.post.PostViewDto
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class PostViewRepositoryImplTest : FunSpec({
  val postViewJpaRepository = mockk<PostViewJpaRepository>()
  val repository = PostViewRepositoryImpl(postViewJpaRepository)

  val today = LocalDate.now()
  val dto = PostViewDto(
    id = Tsid.generate(),
    postId = Tsid.encode(1),
    userOrIp = "aid-1",
    viewedDate = today,
    userAgent = "ua"
  )

  test("insertIfAbsent - 행이 삽입되면 true") {
    every { postViewJpaRepository.insertIfAbsent(any(), 1L, "aid-1", today, "ua") } returns 1

    repository.insertIfAbsent(dto) shouldBe true
    verify(exactly = 1) { postViewJpaRepository.insertIfAbsent(any(), 1L, "aid-1", today, "ua") }
  }

  test("insertIfAbsent - 충돌로 삽입되지 않으면 false") {
    every { postViewJpaRepository.insertIfAbsent(any(), 1L, "aid-1", today, "ua") } returns 0

    repository.insertIfAbsent(dto) shouldBe false
  }
})
