package com.techinsights.api.controller.company

import com.techinsights.api.request.CompanyRequest
import com.techinsights.api.response.CompanyResponse
import com.techinsights.service.CompanyService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class CompanyController(
  private val companyService: CompanyService
){

  @GetMapping("/api/v1/companies")
  fun getCompanyList(pageable: Pageable): ResponseEntity<Page<CompanyResponse>> {
    val companyPage = companyService.listCompanies(pageable)
    val responsePage = companyPage.map { CompanyResponse.fromDto(it) }

    return ResponseEntity.ok(responsePage)
  }

  @PostMapping("/api/v1/companies")
  fun saveCompany(@RequestBody requests : List<CompanyRequest>): ResponseEntity<List<CompanyResponse>> {
    val companyDto = requests.map{it.toDto()}
    val savedCompany = companyService.saveCompany(companyDto)
    val response = savedCompany.map { CompanyResponse.fromDto(it) }
    return ResponseEntity.ok(response)
  }

  @DeleteMapping("/api/v1/companies/{companyId}")
  fun deleteCompany(companyId: String) {
    companyService.deleteCompany(companyId)
  }

  @PutMapping("/api/v1/companies/{companyId}")
  fun updateCompany(@PathVariable companyId: String, @RequestBody companyRequest: CompanyRequest): ResponseEntity<CompanyResponse> {
    val companyDto = companyRequest.toDtoWithId(companyId)
    val savedCompanyDto = companyService.updateCompany(companyDto)
    return ResponseEntity.ok(CompanyResponse.fromDto(savedCompanyDto))
  }
}