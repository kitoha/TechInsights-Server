package com.techinsights.api.response.category

import com.techinsights.domain.dto.catogory.CategorySummaryDto
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
