package com.techinsights.domain.repository.company

import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.entity.company.Company
import com.techinsights.domain.entity.company.QCompany
import com.techinsights.domain.utils.Tsid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class CompanyRepositoryImpl(
  private val companyJpaRepository: CompanyJpaRepository,
  private val jpaQueryFactory: JPAQueryFactory
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
    val entity: Company = companyJpaRepository.findById(id.toLong())
      .orElseThrow { NoSuchElementException("Company with ID $id not found") }
    return CompanyDto.fromEntity(entity)
  }

  override fun getList(pageable: Pageable): Page<CompanyDto> {
    val page: Page<Company> = companyJpaRepository.findAll(pageable)
    val companyDtos: List<CompanyDto> = page.content.map { CompanyDto.fromEntity(it) }
    return PageImpl(companyDtos, pageable, page.totalElements)
  }

  override fun deleteById(id: String) {
    val companyId = Tsid.decode(id)
    companyJpaRepository.deleteById(companyId)
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
}