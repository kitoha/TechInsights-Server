package com.techinsights.api.controller.company

import com.techinsights.api.request.CompanyRequest
import com.techinsights.api.response.company.CompanyPostSummaryResponse
import com.techinsights.api.response.company.CompanyResponse
import com.techinsights.api.response.post.PageResponse
import com.techinsights.domain.service.company.CompanyService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class CompanyController(
  private val companyService: CompanyService
){

  @GetMapping("/api/v1/companies")
  fun getCompanyList(pageable: Pageable): ResponseEntity<PageResponse<CompanyResponse>> {
    val companyPage = companyService.listCompanies(pageable)
    val responsePage = companyPage.map { CompanyResponse.fromDto(it) }

    return ResponseEntity.ok(
      PageResponse(
        content = responsePage.content,
        page = responsePage.number,
        size = responsePage.size,
        totalElements = responsePage.totalElements,
        totalPages = responsePage.totalPages
      )
    )
  }

  @GetMapping("/api/v1/companies/{companyId}")
  fun getCompanyById(@PathVariable companyId: String): ResponseEntity<CompanyResponse> {
    val companyDto = companyService.getCompanyById(companyId)
    return ResponseEntity.ok(CompanyResponse.fromDto(companyDto))
  }

  @PostMapping("/api/v1/companies")
  fun saveCompany(@RequestBody requests : List<CompanyRequest>): ResponseEntity<List<CompanyResponse>> {
    val companyDto = requests.map{it.toDto()}
    val savedCompany = companyService.saveCompany(companyDto)
    val response = savedCompany.map { CompanyResponse.fromDto(it) }
    return ResponseEntity.ok(response)
  }

  @DeleteMapping("/api/v1/companies/{companyId}")
  fun deleteCompany(companyId: String): ResponseEntity<Boolean> {
    return ResponseEntity.ok(companyService.deleteCompany(companyId))
  }

  @PutMapping("/api/v1/companies/{companyId}")
  fun updateCompany(@PathVariable companyId: String, @RequestBody companyRequest: CompanyRequest): ResponseEntity<CompanyResponse> {
    val companyDto = companyRequest.toDtoWithId(companyId)
    val savedCompanyDto = companyService.updateCompany(companyDto)
    return ResponseEntity.ok(CompanyResponse.fromDto(savedCompanyDto))
  }

  @GetMapping("/api/v1/companies/top-by-views")
  fun getTopCompanies(@RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "10") size: Int): ResponseEntity<PageResponse<CompanyResponse>> {
    val pageable = PageRequest.of(page, size)
    val topCompanies = companyService.getTopCompaniesByViews(pageable)
    val response = topCompanies.map { CompanyResponse.fromDto(it) }
    return ResponseEntity.ok(
      PageResponse(
      content = response.content,
      page = response.number,
      size = response.size,
      totalElements = response.totalElements,
      totalPages = response.totalPages
    )
    )
  }

  @GetMapping("/api/v1/companies/top-by-posts")
  fun getTopCompaniesByPosts(@RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "10") size: Int): ResponseEntity<PageResponse<CompanyResponse>> {
    val pageable = PageRequest.of(page, size)
    val topCompanies = companyService.getTopCompaniesByPosts(pageable)
    val response = topCompanies.map { CompanyResponse.fromDto(it) }
    return ResponseEntity.ok(
      PageResponse(
      content = response.content,
      page = response.number,
      size = response.size,
      totalElements = response.totalElements,
      totalPages = response.totalPages
    )
    )
  }

  @GetMapping("/api/v1/companiesSummaries")
  fun getCompanyPostSummaries(): ResponseEntity<List<CompanyPostSummaryResponse>> {
    val summaries = companyService.getCompanyPostSummaries()
    val response = summaries.map { CompanyPostSummaryResponse.fromDto(it) }
    return ResponseEntity.ok(response)
  }
}