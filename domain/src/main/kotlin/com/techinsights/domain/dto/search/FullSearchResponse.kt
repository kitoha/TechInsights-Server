package com.techinsights.domain.dto.search

data class FullSearchResponse(
  val query: String,
  val posts: List<PostSearchResultDto>,
  val totalCount: Long,
  val currentPage: Int,
  val totalPages: Int,
  val hasNext: Boolean
)