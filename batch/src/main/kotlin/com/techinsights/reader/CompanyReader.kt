package com.techinsights.reader

import com.techinsights.dto.company.CompanyDto
import com.techinsights.repository.company.CompanyRepository
import org.springframework.batch.item.ItemReader
import org.springframework.stereotype.Component

@Component
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