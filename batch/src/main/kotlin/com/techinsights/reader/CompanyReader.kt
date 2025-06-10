package com.techinsights.reader

import com.techinsights.entity.company.Company
import com.techinsights.repository.company.CompanyRepository
import org.springframework.batch.item.ItemReader
import org.springframework.stereotype.Component

@Component
class CompanyReader(
  private val companyRepository: CompanyRepository
) : ItemReader<Company> {

  private var iterator: Iterator<Company>? = null

  override fun read(): Company? {
    if (iterator == null) {
      iterator = companyRepository.findAll().iterator()
    }
    return if (iterator!!.hasNext()) iterator!!.next() else null
  }
}