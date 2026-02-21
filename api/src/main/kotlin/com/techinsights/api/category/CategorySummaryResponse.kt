package com.techinsights.api.category

import com.techinsights.domain.dto.category.CategorySummaryDto
import com.techinsights.domain.enums.Category
import java.time.LocalDateTime

data class CategorySummaryResponse(
  val category: Category,
  val postCount: Long,
  val totalViewCount: Long,
  val latestPostDate: LocalDateTime?
) {

  companion object {

    fun from(summaries: List<CategorySummaryDto>): List<CategorySummaryResponse> {
      return summaries.map {
        CategorySummaryResponse(
          category = it.category,
          postCount = it.postCount,
          totalViewCount = it.totalViewCount,
          latestPostDate = it.latestPostDate
        )
      }
    }
  }
}
