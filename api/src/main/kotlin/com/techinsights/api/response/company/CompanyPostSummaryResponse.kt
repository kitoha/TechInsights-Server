package com.techinsights.api.response.company

import com.techinsights.domain.dto.company.CompanyPostSummaryDto
import com.techinsights.domain.utils.Tsid
import java.time.LocalDateTime

data class CompanyPostSummaryResponse(
  val id: String,
  val name: String,
  val blogUrl: String,
  val logoImageName: String,
  val totalViewCount: Long = 0L,
  val postCount: Long = 0L,
  val lastPostedAt: LocalDateTime? = null
) {

  companion object {

    fun fromDto(dto: CompanyPostSummaryDto): CompanyPostSummaryResponse {
      return CompanyPostSummaryResponse(
        id = Tsid.encode(dto.id),
        name = dto.name,
        blogUrl = dto.blogUrl,
        logoImageName = dto.logoImageName,
        totalViewCount = dto.totalViewCount,
        postCount = dto.postCount,
        lastPostedAt = dto.lastPostedAt
      )
    }
  }
}