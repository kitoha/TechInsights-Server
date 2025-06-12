package com.techinsights.batch.reader

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.repository.company.CompanyRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemReader
import org.springframework.stereotype.Component

@Component
@StepScope
class CompanyReader(
  private val companyRepository: CompanyRepository
) : ItemReader<CompanyDto> {

  private var iterator: Iterator<CompanyDto>? = null

  override fun read(): CompanyDto? {
    if (iterator == null) {
      iterator = companyRepository.findAll().iterator()
    }
    return if (iterator!!.hasNext()) iterator!!.next() else null
  }
}