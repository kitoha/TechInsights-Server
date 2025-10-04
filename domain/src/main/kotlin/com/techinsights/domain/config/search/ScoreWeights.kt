package com.techinsights.domain.config.search

data class ScoreWeights(
  val titleExactStart: Double = 100.0,
  val titleContains: Double = 50.0,
  val companyName: Double = 30.0,
  val content: Double = 10.0,
  val viewCountMultiplier: Double = 5.0,
  val recent7days: Double = 20.0,
  val recent30days: Double = 10.0,
  val summaryBonus: Double = 5.0
)