package com.techinsights.service

import com.techinsights.dto.company.CompanyDto
import com.techinsights.exeption.CompanyNotFoundException
import com.techinsights.exeption.DuplicateCompanyNameException
import com.techinsights.repository.company.CompanyRepository
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
  fun saveCompany(companyDto: CompanyDto): CompanyDto {

    // 중복 회사명 체크
    if (companyRepository.existsByName(companyDto.name)) {
      throw DuplicateCompanyNameException("Company with name '${companyDto.name}' already exists")
    }

    return companyRepository.save(companyDto)
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