package com.techinsights.domain.event

data class ViewCountIncrementEvent(
  val postId: String,
  val companyId: String
)
