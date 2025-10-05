package com.techinsights.domain.config.search

import com.fasterxml.jackson.annotation.JsonProperty

data class ScoreWeights(
  @JsonProperty("title-exact-start")
  val titleExactStart: Double = 100.0,
  @JsonProperty("title-contains")
  val titleContains: Double = 50.0,
  @JsonProperty("company-name")
  val companyName: Double = 30.0,
  @JsonProperty("content")
  val content: Double = 10.0,
  @JsonProperty("view-count-multiplier")
  val viewCountMultiplier: Double = 5.0,
  @JsonProperty("recent-7days")
  val recent7days: Double = 20.0,
  @JsonProperty("recent-30days")
  val recent30days: Double = 10.0,
  @JsonProperty("summary-bonus")
  val summaryBonus: Double = 5.0
)