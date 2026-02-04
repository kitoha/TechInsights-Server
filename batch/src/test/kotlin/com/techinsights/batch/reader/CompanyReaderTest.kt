package com.techinsights.batch.reader

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.repository.company.CompanyRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class CompanyReaderTest : FunSpec({

    val companyRepository = mockk<CompanyRepository>()
    lateinit var companyReader: CompanyReader

    beforeEach {
        io.mockk.clearMocks(companyRepository)
        companyReader = CompanyReader(companyRepository)
    }

    test("read should return companies one by one and then null") {
        val company1 = CompanyDto(id = "1", name = "Company 1", blogUrl = "https://blog1.com", logoImageName = "logo1.png")
        val company2 = CompanyDto(id = "2", name = "Company 2", blogUrl = "https://blog2.com", logoImageName = "logo2.png")
        val companies = listOf(company1, company2)

        every { companyRepository.findAll() } returns companies

        companyReader.read() shouldBe company1
        companyReader.read() shouldBe company2
        companyReader.read() shouldBe null

        verify(exactly = 1) { companyRepository.findAll() }
    }

    test("read should return null if no companies found") {
        every { companyRepository.findAll() } returns emptyList()

        companyReader.read() shouldBe null
    }

    test("read should be thread-safe, initialize once, and distribute all items exactly once") {
        val count = 100
        val companies = (1..count).map { 
            CompanyDto(id = it.toString(), name = "Company $it", blogUrl = "https://blog$it.com", logoImageName = "logo$it.png") 
        }
        
        every { companyRepository.findAll() } returns companies

        val results = java.util.Collections.synchronizedList(mutableListOf<CompanyDto>())
        val threads = (1..10).map {
            Thread {
                while (true) {
                    val company = companyReader.read() ?: break
                    results.add(company)
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        verify(exactly = 1) { companyRepository.findAll() }

        results.size shouldBe count

        results.distinctBy { it.id }.size shouldBe count
        results.map { it.id }.toSet() shouldBe companies.map { it.id }.toSet()
    }
})
