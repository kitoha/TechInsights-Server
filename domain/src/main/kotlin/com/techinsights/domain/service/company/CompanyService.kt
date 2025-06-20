package com.techinsights.domain.service.company

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.exeption.CompanyNotFoundException
import com.techinsights.domain.exeption.DuplicateCompanyNameException
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
    } catch (e: Exception) {
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
  fun deleteCompany(companyId: String) {
    if (!companyRepository.existsById(companyId)) {
      throw CompanyNotFoundException("Company with id $companyId not found")
    }

    companyRepository.deleteById(companyId)
  }

  @Transactional
  fun updateCompany(companyDto: CompanyDto): CompanyDto {
    if (!companyRepository.existsById(companyDto.id)) {
      throw CompanyNotFoundException("Company with id ${companyDto.id} not found")
    }
    return companyRepository.update(companyDto)
  }
}