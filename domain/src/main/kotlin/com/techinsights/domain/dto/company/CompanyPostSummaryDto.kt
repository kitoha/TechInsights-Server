package com.techinsights.domain.dto.company

import java.time.LocalDateTime

data class CompanyPostSummaryDto(
  val id: Long,
  val name: String,
  val blogUrl: String,
  val logoImageName: String,
  val totalViewCount: Long,
  val postCount: Long,
  val lastPostedAt: LocalDateTime?
)