package com.techinsights.domain.dto.category

import com.techinsights.domain.enums.Category
import java.time.LocalDateTime

data class CategorySummaryDto(
  val category: Category,
  val postCount: Long,
  val totalViewCount: Long,
  val latestPostDate: LocalDateTime?
)