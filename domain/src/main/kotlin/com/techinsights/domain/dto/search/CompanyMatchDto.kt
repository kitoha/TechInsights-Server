package com.techinsights.domain.dto.search

data class CompanyMatchDto(
  val id: String,
  val name: String,
  val logoImageName: String,
  val postCount: Long,
  val matchedPostCount: Long,
  val highlightedName: String
)