package com.techinsights.domain.service.company

import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.entity.company.QCompany
import com.techinsights.domain.service.company.CompanyViewCountUpdater.Companion.INCREASE_COUNT
import com.techinsights.domain.utils.Tsid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompanyViewCountUpdaterImpl (
  private val jpaQueryFactory: JPAQueryFactory
): CompanyViewCountUpdater {

  @Transactional
  override fun incrementTotalViewCount(companyId: String) {
    val company = QCompany.company

    jpaQueryFactory.update(company)
      .set(company.totalViewCount, company.totalViewCount.add(INCREASE_COUNT))
      .where(company.id.eq(Tsid.decode(companyId)))
      .execute()
  }

  @Transactional
  override fun incrementPostCount(companyId: String, count: Int) {
    val company = QCompany.company

    jpaQueryFactory.update(company)
      .set(company.postCount, company.postCount.add(count))
      .where(company.id.eq(Tsid.decode(companyId)))
      .execute()
  }

}