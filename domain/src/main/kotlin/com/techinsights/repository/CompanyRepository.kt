package com.techinsights.repository

import com.techinsights.entity.Company
import org.springframework.data.jpa.repository.JpaRepository

interface CompanyRepository : JpaRepository<Company, Long> {
}