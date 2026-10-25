package com.techinsights.api.search

import com.techinsights.domain.dto.search.PostMatchDto
import com.techinsights.domain.enums.Category
import java.time.LocalDateTime

data class PostMatchResponse(
  val id: String,
  val title: String,
  val companyName: String,
  val companyLogo: String,
  val viewCount: Long,
  val publishedAt: LocalDateTime,
  val highlightedTitle: String,
  val categories: Set<Category>
) {

  companion object {
    fun fromDto(dto: PostMatchDto): PostMatchResponse = PostMatchResponse(
      id = dto.id,
      title = dto.title,
      companyName = dto.companyName,
      companyLogo = dto.companyLogo,
      viewCount = dto.viewCount,
      publishedAt = dto.publishedAt,
      highlightedTitle = dto.highlightedTitle,
      categories = dto.categories
    )
  }
}
