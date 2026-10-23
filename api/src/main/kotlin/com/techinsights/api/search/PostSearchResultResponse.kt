package com.techinsights.api.search

import com.techinsights.domain.dto.search.PostSearchResultDto
import com.techinsights.domain.enums.Category
import java.time.LocalDateTime

data class PostSearchResultResponse(
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
) {

  companion object {
    fun fromDto(dto: PostSearchResultDto): PostSearchResultResponse = PostSearchResultResponse(
      id = dto.id,
      title = dto.title,
      preview = dto.preview,
      url = dto.url,
      thumbnail = dto.thumbnail,
      companyId = dto.companyId,
      companyName = dto.companyName,
      companyLogo = dto.companyLogo,
      viewCount = dto.viewCount,
      publishedAt = dto.publishedAt,
      isSummary = dto.isSummary,
      categories = dto.categories,
      relevanceScore = dto.relevanceScore
    )
  }
}
