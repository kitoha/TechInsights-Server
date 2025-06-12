package com.techinsights.domain.repository.company

import com.techinsights.domain.entity.company.Company
import org.springframework.data.jpa.repository.JpaRepository

interface CompanyJpaRepository : JpaRepository<Company, Long> {
  fun existsByName(name: String): Boolean
  fun findAllByNameIn(names: List<String>): List<Company>
}