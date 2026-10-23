package com.techinsights.api.search

import com.techinsights.domain.dto.search.CompanyMatchDto

data class CompanyMatchResponse(
  val id: String,
  val name: String,
  val logoImageName: String,
  val postCount: Long,
  val matchedPostCount: Long,
  val highlightedName: String
) {

  companion object {
    fun fromDto(dto: CompanyMatchDto): CompanyMatchResponse = CompanyMatchResponse(
      id = dto.id,
      name = dto.name,
      logoImageName = dto.logoImageName,
      postCount = dto.postCount,
      matchedPostCount = dto.matchedPostCount,
      highlightedName = dto.highlightedName
    )
  }
}
