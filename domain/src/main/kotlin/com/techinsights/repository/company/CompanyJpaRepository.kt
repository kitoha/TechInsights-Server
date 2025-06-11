package com.techinsights.repository.company

import com.techinsights.entity.company.Company
import org.springframework.data.jpa.repository.JpaRepository

interface CompanyJpaRepository : JpaRepository<Company, Long> {
  fun existsByName(name: String): Boolean
  fun findAllByNameIn(names: List<String>): List<Company>
}