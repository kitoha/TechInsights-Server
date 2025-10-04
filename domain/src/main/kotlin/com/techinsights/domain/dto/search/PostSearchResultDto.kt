package com.techinsights.domain.dto.search

import com.techinsights.domain.enums.Category
import java.time.LocalDateTime

data class PostSearchResultDto(
  val id: String,
  val title: String,
  val preview: String?,
  val url: String,
  val thumbnail: String?,
  val companyId: String,
  val companyName: String,
  val companyLogo: String,
  val viewCount: Long,
  val publishedAt: LocalDateTime,
  val isSummary: Boolean,
  val categories: Set<Category>,
  val relevanceScore: Double
)