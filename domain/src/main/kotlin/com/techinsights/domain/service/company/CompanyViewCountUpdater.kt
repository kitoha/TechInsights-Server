package com.techinsights.domain.service.company

interface CompanyViewCountUpdater {
  fun incrementTotalViewCount(companyId: String)
  fun incrementPostCount(companyId: String, count: Int)

  companion object {
    const val INCREASE_COUNT = 1
  }
}