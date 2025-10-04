package com.techinsights.domain.dto.search

import com.techinsights.domain.enums.Category
import java.time.LocalDateTime

data class PostMatchDto(
  val id: String,
  val title: String,
  val companyName: String,
  val companyLogo: String,
  val viewCount: Long,
  val publishedAt: LocalDateTime,
  val highlightedTitle: String,
  val categories: Set<Category>
)