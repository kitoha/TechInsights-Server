package com.techinsights.api.company

import com.techinsights.domain.exception.CompanyNotFoundException
import com.techinsights.domain.service.company.CompanyService
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import com.techinsights.api.exception.GlobalExceptionHandler
import com.techinsights.domain.dto.company.CompanyPostSummaryDto
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class CompanyControllerTest : FunSpec({
  val companyService = mockk<CompanyService>()
  val mockMvc = MockMvcBuilders.standaloneSetup(CompanyController(companyService))
    .setControllerAdvice(GlobalExceptionHandler())
    .build()

  test("DELETE /api/v1/companies/{companyId} - 경로 변수로 회사를 삭제한다") {
    every { companyService.deleteCompany("company-1") } returns true

    mockMvc.delete("/api/v1/companies/company-1")
      .andExpect {
        status { isOk() }
        content { string("true") }
      }

    verify(exactly = 1) { companyService.deleteCompany("company-1") }
  }

  val summary = CompanyPostSummaryDto(
    id = 1L, name = "Toss", blogUrl = "https://toss.tech", logoImageName = "toss.png",
    totalViewCount = 10, postCount = 2, lastPostedAt = null
  )

  test("GET /api/v1/companies/summaries - 정식 경로, {companyId} 와 충돌하지 않고 Deprecation 헤더가 없다") {
    every { companyService.getCompanyPostSummaries() } returns listOf(summary)

    mockMvc.get("/api/v1/companies/summaries")
      .andExpect {
        status { isOk() }
        jsonPath("$[0].name") { value("Toss") }
        header { doesNotExist("Deprecation") }
        header { doesNotExist("Sunset") }
      }

    verify(exactly = 0) { companyService.getCompanyById(any()) }
  }

  test("GET /api/v1/companiesSummaries - 구 경로는 같은 응답에 Deprecation/Sunset 헤더를 붙인다") {
    every { companyService.getCompanyPostSummaries() } returns listOf(summary)

    mockMvc.get("/api/v1/companiesSummaries")
      .andExpect {
        status { isOk() }
        jsonPath("$[0].name") { value("Toss") }
        header { string("Deprecation", "true") }
        header { string("Sunset", "Sun, 22 Nov 2026 00:00:00 GMT") }
      }
  }

  test("GET /api/v1/companies/{companyId} - summaries 가 아닌 값은 여전히 회사 조회") {
    every { companyService.getCompanyById("abc") } throws CompanyNotFoundException("abc")

    mockMvc.get("/api/v1/companies/abc").andExpect { status { isNotFound() } }

    verify(exactly = 1) { companyService.getCompanyById("abc") }
  }
})
