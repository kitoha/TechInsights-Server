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

  private val log = org.slf4j.LoggerFactory.getLogger(javaClass)
  private var iterator: Iterator<CompanyDto>? = null

  override fun read(): CompanyDto? = synchronized(this) {
    val threadName = Thread.currentThread().name
    if (iterator == null) {
      val companies = companyRepository.findAll()
      log.info("[{}] CompanyReader initialized with {} companies", threadName, companies.size)
      iterator = companies.iterator()
    }

    val company = if (iterator!!.hasNext()) iterator!!.next() else null
    if (company != null) {
      log.debug("[{}] Read company: {} ({})", threadName, company.name, company.blogUrl)
    }
    company
  }
}