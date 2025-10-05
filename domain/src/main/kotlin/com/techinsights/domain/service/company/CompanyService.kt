package com.techinsights.domain.service.company

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.company.CompanyPostSummaryDto
import com.techinsights.domain.exception.CompanyNotFoundException
import com.techinsights.domain.exception.DuplicateCompanyNameException
import com.techinsights.domain.repository.company.CompanyRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompanyService(
  private val companyRepository: CompanyRepository
) {

  @Transactional(readOnly = true)
  fun getCompanyDetails(companyId: String): CompanyDto {
    try {
      return companyRepository.findById(companyId)
    } catch (e: NoSuchElementException) {
      throw CompanyNotFoundException("Company with id $companyId not found")
    }
  }

  @Transactional(readOnly = true)
  fun listCompanies(pageable: Pageable): Page<CompanyDto> {
    return companyRepository.getList(pageable)
  }

  @Transactional
  fun saveCompany(companyDtos: List<CompanyDto>): List<CompanyDto> {
    val names = companyDtos.map { it.name }
    val existingNames = companyRepository.findAllByNameIn(names).map { it.name }.toSet()
    val newCompanyDtos = companyDtos.filter { it.name !in existingNames }
    return try{
      companyRepository.saveAll(newCompanyDtos)
    }catch (e: DataIntegrityViolationException) {
      throw DuplicateCompanyNameException("Duplicate company name found")
    }
  }

  @Transactional
  fun deleteCompany(companyId: String) : Boolean {
    return companyRepository.deleteById(companyId)
  }

  @Transactional
  fun updateCompany(companyDto: CompanyDto): CompanyDto {
    try {
      return companyRepository.update(companyDto)
    }catch (e: NoSuchElementException){
      throw CompanyNotFoundException("Company with id ${companyDto.id} not found")
    } catch (e: DataIntegrityViolationException) {
      throw DuplicateCompanyNameException("Duplicate company name found")
    }
  }

  fun getTopCompaniesByViews(pageable: Pageable): Page<CompanyDto> {
    return companyRepository.getTopCompaniesByViews(pageable)
  }

  fun getTopCompaniesByPosts(pageable: Pageable): Page<CompanyDto> {
    return companyRepository.getTopCompaniesByPosts(pageable)
  }

  fun getCompanyPostSummaries(): List<CompanyPostSummaryDto> {
    return companyRepository.findAllWithLastPostedAt()
  }
}