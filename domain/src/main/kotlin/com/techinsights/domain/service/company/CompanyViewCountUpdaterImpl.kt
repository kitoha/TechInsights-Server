package com.techinsights.domain.service.company

import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.entity.company.QCompany
import com.techinsights.domain.service.company.CompanyViewCountUpdater.Companion.INCREASE_COUNT
import com.techinsights.domain.utils.Tsid
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompanyViewCountUpdaterImpl (
  private val jpaQueryFactory: JPAQueryFactory,
  private val entityManager: EntityManager
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

  @Transactional
  override fun reconcilePostCounts(): Int =
    entityManager.createNativeQuery(RECONCILE_POST_COUNTS_SQL).executeUpdate()

  companion object {
    private const val RECONCILE_POST_COUNTS_SQL = """
      UPDATE company c
      SET post_count = COALESCE(s.cnt, 0)
      FROM company target
      LEFT JOIN (
        SELECT company_id, COUNT(*) AS cnt
        FROM posts
        WHERE is_summary = true AND deleted_at IS NULL
        GROUP BY company_id
      ) s ON s.company_id = target.id
      WHERE c.id = target.id
        AND c.post_count <> COALESCE(s.cnt, 0)
    """
  }

}