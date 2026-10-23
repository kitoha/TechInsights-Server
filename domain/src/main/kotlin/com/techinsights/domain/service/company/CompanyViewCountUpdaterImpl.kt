package com.techinsights.domain.service.company

import com.techinsights.domain.repository.company.CompanyRepository
import com.techinsights.domain.utils.Tsid
import org.springframework.stereotype.Service

@Service
class CompanyViewCountUpdaterImpl (
  private val companyRepository: CompanyRepository
): CompanyViewCountUpdater {

  override fun incrementTotalViewCount(companyId: String) {
    companyRepository.incrementTotalViewCount(Tsid.decode(companyId))
  }

  override fun incrementPostCount(companyId: String, count: Int) {
    companyRepository.incrementPostCount(Tsid.decode(companyId), count)
  }

  override fun reconcilePostCounts(): Int = companyRepository.reconcilePostCounts()

}