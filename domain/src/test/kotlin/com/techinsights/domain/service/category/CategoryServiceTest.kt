package com.techinsights.domain.service.category

import com.techinsights.domain.dto.catogory.CategorySummaryDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.repository.post.PostRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class CategoryServiceTest : FunSpec({
  val postRepository = mockk<PostRepository>()
  val categoryService = CategoryService(postRepository)

  val sampleCategorySummaries = listOf(
    CategorySummaryDto(
      category = Category.FrontEnd,
      postCount = 10,
      totalViewCount = 100,
      latestPostDate = LocalDateTime.now()
    ),
    CategorySummaryDto(
      category = Category.BackEnd,
      postCount = 15,
      totalViewCount = 200,
      latestPostDate = LocalDateTime.now()
    ),
    CategorySummaryDto(
      category = Category.AI,
      postCount = 5,
      totalViewCount = 50,
      latestPostDate = LocalDateTime.now()
    )
  )

  test("카테고리 통계 조회 - 성공") {
    every { postRepository.getCategoryStatistics() } returns sampleCategorySummaries

    val result = categoryService.getCategoryStatistics()

    result shouldHaveSize 3
    result[0].category shouldBe Category.FrontEnd
    result[0].postCount shouldBe 10
    result[1].category shouldBe Category.BackEnd
    result[1].postCount shouldBe 15
    result[2].category shouldBe Category.AI
    result[2].postCount shouldBe 5

    verify(exactly = 1) { postRepository.getCategoryStatistics() }
  }
})



