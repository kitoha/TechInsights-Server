package com.techinsights.api.search

import com.techinsights.domain.dto.search.FullSearchResponse as FullSearchResult

data class FullSearchResponse(
  val query: String,
  val posts: List<PostSearchResultResponse>,
  val totalCount: Long,
  val currentPage: Int,
  val totalPages: Int,
  val hasNext: Boolean
) {

  companion object {
    fun fromDto(dto: FullSearchResult): FullSearchResponse = FullSearchResponse(
      query = dto.query,
      posts = dto.posts.map { PostSearchResultResponse.fromDto(it) },
      totalCount = dto.totalCount,
      currentPage = dto.currentPage,
      totalPages = dto.totalPages,
      hasNext = dto.hasNext
    )
  }
}
