package com.techinsights.api.company

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import com.techinsights.api.config.MAX_PAGE_SIZE
import com.techinsights.api.post.PageResponse
import com.techinsights.domain.service.company.CompanyService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
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
  fun deleteCompany(@PathVariable companyId: String): ResponseEntity<Boolean> {
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
    @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) size: Int): ResponseEntity<PageResponse<CompanyResponse>> {
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
    @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) size: Int): ResponseEntity<PageResponse<CompanyResponse>> {
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

  @GetMapping("/api/v1/companies/summaries")
  fun getCompanyPostSummaries(): ResponseEntity<List<CompanyPostSummaryResponse>> {
    val summaries = companyService.getCompanyPostSummaries()
    val response = summaries.map { CompanyPostSummaryResponse.fromDto(it) }
    return ResponseEntity.ok(response)
  }

  @Deprecated("Renamed. Use GET /api/v1/companies/summaries.")
  @GetMapping("/api/v1/companiesSummaries")
  fun getCompanyPostSummariesLegacy(): ResponseEntity<List<CompanyPostSummaryResponse>> {
    val headers = HttpHeaders()
    headers.set("Deprecation", "true")
    headers.set("Sunset", LEGACY_SUMMARIES_SUNSET)
    return ResponseEntity.ok().headers(headers).body(getCompanyPostSummaries().body)
  }

  companion object {
    /** The old `/companiesSummaries` path is removed after this date (one release window). */
    private const val LEGACY_SUMMARIES_SUNSET = "Sun, 22 Nov 2026 00:00:00 GMT"
  }
}
