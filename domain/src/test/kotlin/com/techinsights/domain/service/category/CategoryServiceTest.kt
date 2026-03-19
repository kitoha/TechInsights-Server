package com.techinsights.domain.service.category

import com.techinsights.domain.dto.category.CategorySummaryDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.repository.post.PostRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class CategoryServiceTest : FunSpec({
  val postRepository = mockk<PostRepository>()
  val categoryService = CategoryService(postRepository)

  beforeTest { clearAllMocks() }

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
    every { postRepository.getAllPostSummary() } returns CategorySummaryDto(
      category = Category.All,
      postCount = 20,
      totalViewCount = 250,
      latestPostDate = LocalDateTime.now()
    )
    every { postRepository.getCategoryStatistics() } returns sampleCategorySummaries

    val result = categoryService.getCategoryStatistics()

    result shouldHaveSize 4
    result[0].category shouldBe Category.All
    result[1].category shouldBe Category.FrontEnd
    result[1].postCount shouldBe 10
    result[2].category shouldBe Category.BackEnd
    result[2].postCount shouldBe 15
    result[3].category shouldBe Category.AI
    result[3].postCount shouldBe 5

    verify(exactly = 1) { postRepository.getAllPostSummary() }
    verify(exactly = 1) { postRepository.getCategoryStatistics() }
  }

  test("카테고리 통계 조회 - All이 첫 번째이며 고유 집계값이다") {
    val allSummary = CategorySummaryDto(
      category = Category.All,
      postCount = 20,
      totalViewCount = 250,
      latestPostDate = LocalDateTime.now()
    )

    every { postRepository.getAllPostSummary() } returns allSummary
    every { postRepository.getCategoryStatistics() } returns sampleCategorySummaries

    val result = categoryService.getCategoryStatistics()

    result shouldHaveSize 4
    result[0].category shouldBe Category.All
    result[0].postCount shouldBe 20
    result[0].totalViewCount shouldBe 250
    result[0].postCount shouldBeLessThan result.drop(1).sumOf { it.postCount }

    verify(exactly = 1) { postRepository.getAllPostSummary() }
    verify(exactly = 1) { postRepository.getCategoryStatistics() }
  }
})



