package com.techinsights.domain.repository.search

import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.entity.company.Company
import com.techinsights.domain.entity.company.QCompany
import org.springframework.stereotype.Repository

@Repository
class CompanySearchQueryRepository(
  private val jpaQueryFactory: JPAQueryFactory
) {

  private val company = QCompany.company

  fun findByNameContaining(query: String, limit: Int): List<Company> {
    return jpaQueryFactory
      .selectFrom(company)
      .where(company.name.containsIgnoreCase(query))
      .orderBy(company.totalViewCount.desc())
      .limit(limit.toLong())
      .fetch()
  }

  fun findIdsByNameContaining(query: String): List<Long> {
    return jpaQueryFactory
      .select(company.id)
      .from(company)
      .where(company.name.containsIgnoreCase(query))
      .fetch()
  }
}