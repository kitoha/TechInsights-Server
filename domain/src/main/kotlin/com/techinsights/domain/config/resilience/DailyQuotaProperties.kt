package com.techinsights.domain.config.resilience

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "daily-quota")
data class DailyQuotaProperties(
  val zone: String = "America/Los_Angeles",
  val limits: Limits = Limits()
) {

  data class Limits(
    val batchSummary: Int = 20,
    val readmeSummary: Int = 20,
    val communityAnalysis: Int = 20,
    // gemini-embedding-001 무료 티어 RPD. 구글 공식 문서로 검증하지 못했고, 기존 batch 설정 주석(github-embedding)의 1,000 RPD를 따른다.
    val embedding: Int = 1000
  )
}
