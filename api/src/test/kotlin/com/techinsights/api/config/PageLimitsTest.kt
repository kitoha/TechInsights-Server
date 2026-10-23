package com.techinsights.api.config

import com.techinsights.api.company.CompanyController
import com.techinsights.api.exception.GlobalExceptionHandler
import com.techinsights.api.post.PostController
import com.techinsights.domain.service.company.CompanyService
import com.techinsights.domain.service.post.PostService
import com.techinsights.domain.service.post.PostViewService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class PageLimitsTest : FunSpec({
    val postService = mockk<PostService>()
    val companyService = mockk<CompanyService>()

    val pageableResolver = PageableHandlerMethodArgumentResolver().also {
        PageLimits().pageableMaxSizeCustomizer().customize(it)
    }
    val mockMvc = MockMvcBuilders
        .standaloneSetup(PostController(postService, mockk<PostViewService>()), CompanyController(companyService))
        .setCustomArgumentResolvers(pageableResolver)
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    test("Pageable 의 size 는 100 으로 제한된다") {
        val pageable = slot<Pageable>()
        every { companyService.listCompanies(capture(pageable)) } returns PageImpl(emptyList())

        mockMvc.get("/api/v1/companies") { param("size", "100000") }.andExpect { status { isOk() } }

        pageable.captured.pageSize shouldBe MAX_PAGE_SIZE.toInt()
    }

    test("size 파라미터가 상한을 넘으면 400") {
        mockMvc.get("/api/v1/posts") { param("size", "101") }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_PARAMETER") }
        }
    }

    test("size 파라미터가 0 이하이면 400") {
        mockMvc.get("/api/v1/posts") { param("size", "0") }.andExpect { status { isBadRequest() } }
    }

    test("size 파라미터가 상한 이하이면 통과한다 (기본값 유지)") {
        every { postService.getPosts(0, 10, any(), any(), null) } returns PageImpl(emptyList())
        every { postService.getPosts(0, 100, any(), any(), null) } returns PageImpl(emptyList())

        mockMvc.get("/api/v1/posts").andExpect { status { isOk() } }
        mockMvc.get("/api/v1/posts") { param("size", "100") }.andExpect { status { isOk() } }
    }

    test("회사 top 목록 size 도 상한을 넘으면 400") {
        mockMvc.get("/api/v1/companies/top-by-views") { param("size", "5000") }.andExpect { status { isBadRequest() } }
        mockMvc.get("/api/v1/companies/top-by-posts") { param("size", "5000") }.andExpect { status { isBadRequest() } }
    }
})
