package com.techinsights.domain.dto.summary

data class SummaryResult (
  val summary: String,
  val categories: List<String>,
  val preview: String?
)