package com.techinsights.domain.repository.company

import com.techinsights.domain.dto.company.CompanyDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CompanyRepository {
  fun findAll(): List<CompanyDto>
  fun save(company: CompanyDto): CompanyDto
  fun saveAll(companies: List<CompanyDto>): List<CompanyDto>
  fun findById(id: String): CompanyDto
  fun getList(pageable: Pageable): Page<CompanyDto>
  fun deleteById(id: String) : Boolean
  fun update(company: CompanyDto): CompanyDto
  fun existsById(id: String): Boolean
  fun findAllByNameIn(names: List<String>): List<CompanyDto>
  fun existsByName(name: String): Boolean
  fun getTopCompaniesByViews(pageable: Pageable): Page<CompanyDto>
  fun getTopCompaniesByPosts(pageable: Pageable): Page<CompanyDto>
}