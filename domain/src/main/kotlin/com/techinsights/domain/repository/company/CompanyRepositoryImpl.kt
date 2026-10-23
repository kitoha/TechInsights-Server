package com.techinsights.domain.repository.company

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.company.CompanyPostSummaryDto
import com.techinsights.domain.entity.company.Company
import com.techinsights.domain.entity.company.QCompany
import com.techinsights.domain.entity.company.QCompany.company
import com.techinsights.domain.entity.post.QPost.post
import com.techinsights.domain.utils.Tsid
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class CompanyRepositoryImpl(
  private val companyJpaRepository: CompanyJpaRepository,
  private val jpaQueryFactory: JPAQueryFactory,
  private val entityManager: EntityManager
) : CompanyRepository {

  override fun findAll(): List<CompanyDto> {
    val companies: List<Company> = companyJpaRepository.findAll()
    return companies.map { CompanyDto.fromEntity(it) }
  }

  override fun save(company: CompanyDto): CompanyDto {
    val entity = company.toEntity()
    val savedEntity: Company = companyJpaRepository.save(entity)
    return CompanyDto.fromEntity(savedEntity)
  }

  override fun saveAll(companies: List<CompanyDto>): List<CompanyDto> {
    val entities: List<Company> = companies.map { it.toEntity() }
    val savedEntities: List<Company> = companyJpaRepository.saveAll(entities)
    return savedEntities.map { CompanyDto.fromEntity(it) }
  }

  override fun findById(id: String): CompanyDto {
    val companyId = Tsid.decode(id)
    val entity: Company = companyJpaRepository.findById(companyId)
      .orElseThrow { NoSuchElementException("Company with ID $id not found") }
    return CompanyDto.fromEntity(entity)
  }

  override fun getList(pageable: Pageable): Page<CompanyDto> {
    val page: Page<Company> = companyJpaRepository.findAll(pageable)
    val companyDtos: List<CompanyDto> = page.content.map { CompanyDto.fromEntity(it) }
    return PageImpl(companyDtos, pageable, page.totalElements)
  }

  override fun deleteById(id: String) : Boolean {
    val companyId = Tsid.decode(id)
    if (companyJpaRepository.existsById(companyId)) {
      companyJpaRepository.deleteById(companyId)
      return true
    }
    return false
  }

  override fun update(company: CompanyDto): CompanyDto {
    val entity = company.toEntity()
    if (!companyJpaRepository.existsById(entity.id)) {
      throw NoSuchElementException("Company with ID ${company.id} not found")
    }
    val updatedEntity: Company = companyJpaRepository.save(entity)
    return CompanyDto.fromEntity(updatedEntity)
  }

  override fun existsById(id: String): Boolean {
    return companyJpaRepository.existsById(Tsid.decode(id))
  }

  override fun findAllByNameIn(names: List<String>): List<CompanyDto> {
    val companies: List<Company> = companyJpaRepository.findAllByNameIn(names)
    return companies.map { CompanyDto.fromEntity(it) }
  }

  override fun existsByName(name: String): Boolean {
    return companyJpaRepository.existsByName(name)
  }

  override fun getTopCompaniesByViews(pageable: Pageable): Page<CompanyDto> {
    val company = QCompany.company

    val query = jpaQueryFactory
      .selectFrom(company)
      .orderBy(company.totalViewCount.desc())
      .offset(pageable.offset)
      .limit(pageable.pageSize.toLong())

    val results = query.fetch()

    val total = jpaQueryFactory
      .select(company.count())
      .from(company)
      .fetchOne() ?: 0L

    val companyDtos = results.map { CompanyDto.fromEntity(it) }

    return PageImpl(companyDtos, pageable, total)
  }

  override fun getTopCompaniesByPosts(pageable: Pageable): Page<CompanyDto> {
    val company = QCompany.company

    val query = jpaQueryFactory
      .selectFrom(company)
      .orderBy(company.postCount.desc())
      .offset(pageable.offset)
      .limit(pageable.pageSize.toLong())

    val results = query.fetch()

    val total = jpaQueryFactory
      .select(company.count())
      .from(company)
      .fetchOne() ?: 0L

    val companyDtos = results.map { CompanyDto.fromEntity(it) }

    return PageImpl(companyDtos, pageable, total)
  }

  override fun findAllWithLastPostedAt(): List<CompanyPostSummaryDto> {
    return jpaQueryFactory
      .select(
        Projections.constructor(
          CompanyPostSummaryDto::class.java,
          company.id,
          company.name,
          company.blogUrl,
          company.logoImageName,
          company.totalViewCount,
          post.id.count(),
          post.publishedAt.max()
        )
      )
      .from(company)
      .leftJoin(post).on(
        post.company.id.eq(company.id)
          .and(post.isSummary.isTrue)
      )
      .groupBy(
        company.id,
        company.name,
        company.blogUrl,
        company.logoImageName,
        company.totalViewCount
      )
      .orderBy(post.id.count().desc())
      .fetch()
  }

  @Transactional
  override fun incrementTotalViewCount(companyId: Long) {
    jpaQueryFactory.update(company)
      .set(company.totalViewCount, company.totalViewCount.add(1))
      .where(company.id.eq(companyId))
      .execute()
  }

  @Transactional
  override fun incrementPostCount(companyId: Long, count: Int) {
    jpaQueryFactory.update(company)
      .set(company.postCount, company.postCount.add(count))
      .where(company.id.eq(companyId))
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
