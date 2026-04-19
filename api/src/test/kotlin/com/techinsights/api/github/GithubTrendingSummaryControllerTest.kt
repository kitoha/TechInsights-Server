package com.techinsights.api.github

import com.techinsights.domain.dto.github.GithubSummaryDto
import com.techinsights.domain.enums.GithubSortType
import com.techinsights.domain.service.github.GithubTrendingService
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.mockk
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class GithubTrendingSummaryControllerTest : FunSpec({

    val githubTrendingService = mockk<GithubTrendingService>()
    val controller = GithubTrendingSummaryController(githubTrendingService)
    val mockMvc = MockMvcBuilders.standaloneSetup(controller).build()

    test("요약 정보 조회 API는 정상적으로 요약 데이터를 반환한다") {
        val summary = GithubSummaryDto(100L, 50000L)
        coEvery { githubTrendingService.getSummary(GithubSortType.STARS, "Java") } returns summary

        val asyncResult = mockMvc.get("/api/v1/github/trending/summary") {
            param("sort", "STARS")
            param("language", "Java")
            accept = MediaType.APPLICATION_JSON
        }.andReturn()

        mockMvc.perform(asyncDispatch(asyncResult))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalRepositories").value(100))
            .andExpect(jsonPath("$.totalStars").value(50000))
    }
})
