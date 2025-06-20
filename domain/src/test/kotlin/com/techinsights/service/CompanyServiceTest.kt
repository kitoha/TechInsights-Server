package com.techinsights.service

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.exeption.CompanyNotFoundException
import com.techinsights.domain.exeption.DuplicateCompanyNameException
import com.techinsights.domain.repository.company.CompanyRepository
import com.techinsights.domain.service.company.CompanyService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class CompanyServiceTest : FunSpec({
    val companyRepository = mockk<CompanyRepository>()
    val companyService = CompanyService(companyRepository)

    val sampleCompanyDto = CompanyDto(
        id = "1",
        name = "Test Company",
        blogUrl = "http://testcompany.com/blog",
        logoImageName = "http://testcompany.com/logo.png",
    )

    test("회사 상세 정보 조회 - 성공") {
        every { companyRepository.findById("1") } returns sampleCompanyDto

        val result = companyService.getCompanyDetails("1")
        result shouldBe sampleCompanyDto
    }

    test("회사 상세 정보 조회 - 실패") {
        every { companyRepository.findById("999") } throws Exception()

        shouldThrow<CompanyNotFoundException> {
            companyService.getCompanyDetails("999")
        }
    }

    test("회사 목록 조회") {
        val pageable = PageRequest.of(0, 10)
        val page = PageImpl(listOf(sampleCompanyDto))

        every { companyRepository.getList(pageable) } returns page

        val result = companyService.listCompanies(pageable)
        result shouldBe page
    }

    test("회사 저장 - 성공") {
        every { companyRepository.findAllByNameIn(any()) } returns emptyList() // DB에 중복 없음
        every { companyRepository.saveAll(any<List<CompanyDto>>()) } returns listOf(sampleCompanyDto)

        val result = companyService.saveCompany(listOf(sampleCompanyDto))

        result shouldBe listOf(sampleCompanyDto)
    }

    test("회사 저장 - 중복 회사명(사전 중복)") {
        every { companyRepository.findAllByNameIn(any()) } returns listOf(sampleCompanyDto)
        every { companyRepository.saveAll(emptyList()) } returns emptyList()

        val result = companyService.saveCompany(listOf(sampleCompanyDto))
        result shouldBe emptyList()
    }

    test("회사 저장 - 중복 회사명(저장시 unique 제약 위반)") {
        every { companyRepository.findAllByNameIn(any()) } returns emptyList()
        every { companyRepository.saveAll(any<List<CompanyDto>>()) } throws DataIntegrityViolationException("Duplicate")

        shouldThrow<DuplicateCompanyNameException> {
            companyService.saveCompany(listOf(sampleCompanyDto))
        }
    }

    test("회사 삭제 - 성공") {
        every { companyRepository.existsById("1") } returns true
        every { companyRepository.deleteById("1") } returns Unit

        companyService.deleteCompany("1")
        verify { companyRepository.deleteById("1") }
    }

    test("회사 삭제 - 존재하지 않는 회사") {
        every { companyRepository.existsById("999") } returns false

        shouldThrow<CompanyNotFoundException> {
            companyService.deleteCompany("999")
        }
    }

    test("회사 수정 - 성공") {
        every { companyRepository.existsById("1") } returns true
        every { companyRepository.update(sampleCompanyDto) } returns sampleCompanyDto

        val result = companyService.updateCompany(sampleCompanyDto)
        result shouldBe sampleCompanyDto
    }

    test("회사 수정 - 존재하지 않는 회사") {
        every { companyRepository.existsById("1") } returns false

        shouldThrow<CompanyNotFoundException> {
            companyService.updateCompany(sampleCompanyDto)
        }
    }
})