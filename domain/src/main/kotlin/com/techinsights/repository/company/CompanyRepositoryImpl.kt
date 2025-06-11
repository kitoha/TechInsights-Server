package com.techinsights.repository.company

import com.techinsights.dto.company.CompanyDto
import com.techinsights.entity.company.Company
import com.techinsights.utils.Tsid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class CompanyRepositoryImpl(
  private val companyJpaRepository: CompanyJpaRepository
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

  override fun existsByName(name: String): Boolean {
    return companyJpaRepository.existsByName(name)
  }
}