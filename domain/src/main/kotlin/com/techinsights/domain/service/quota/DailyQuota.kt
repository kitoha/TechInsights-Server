package com.techinsights.domain.service.quota

enum class DailyQuota(val key: String) {
  BATCH_SUMMARY("geminiBatchRpd"),
  README_SUMMARY("geminiReadmeRpd"),
  COMMUNITY_ANALYSIS("geminiCommunityRpd")
}
