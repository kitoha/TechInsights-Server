package com.techinsights.domain.dto.gemini

data class SummaryResult (
  val summary: String,
  val categories: List<String>
)