package com.techinsights.api.company

import com.techinsights.domain.service.company.CompanyService
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class CompanyControllerTest : FunSpec({
  val companyService = mockk<CompanyService>()
  val mockMvc = MockMvcBuilders.standaloneSetup(CompanyController(companyService)).build()

  test("DELETE /api/v1/companies/{companyId} - 경로 변수로 회사를 삭제한다") {
    every { companyService.deleteCompany("company-1") } returns true

    mockMvc.delete("/api/v1/companies/company-1")
      .andExpect {
        status { isOk() }
        content { string("true") }
      }

    verify(exactly = 1) { companyService.deleteCompany("company-1") }
  }
})
