package com.techinsights.api

import com.techinsights.api.github.GithubTrendingController
import com.techinsights.api.github.GithubTrendingSummaryController
import com.techinsights.api.search.SearchController
import com.techinsights.domain.config.search.SemanticSearchProperties
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.github.GithubSemanticSearchResult
import com.techinsights.domain.dto.github.GithubSummaryDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.dto.search.CompanyMatchDto
import com.techinsights.domain.dto.search.FullSearchResponse
import com.techinsights.domain.dto.search.InstantSearchResponse
import com.techinsights.domain.dto.search.PostMatchDto
import com.techinsights.domain.dto.search.PostSearchResultDto
import com.techinsights.domain.dto.search.SemanticSearchResult
import com.techinsights.domain.enums.Category
import com.techinsights.domain.service.github.GithubSemanticSearchService
import com.techinsights.domain.service.github.GithubTrendingService
import com.techinsights.domain.service.search.SearchService
import com.techinsights.domain.service.search.SemanticSearchService
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

/**
 * Pins the exact JSON of endpoints whose payloads are mapped from domain DTOs, so the API contract
 * cannot drift when domain classes change.
 */
class ResponseContractTest : FunSpec({
  val searchService = mockk<SearchService>()
  val semanticSearchService = mockk<SemanticSearchService>()
  val githubTrendingService = mockk<GithubTrendingService>()
  val githubSemanticSearchService = mockk<GithubSemanticSearchService>()

  // Same module set / date format as Spring Boot's auto-configured ObjectMapper
  val converter = MappingJackson2HttpMessageConverter(
    JsonMapper.builder().findAndAddModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build()
  )

  val searchMvc = MockMvcBuilders.standaloneSetup(
    SearchController(searchService, semanticSearchService, SemanticSearchProperties(defaultSize = 10, maxSize = 20))
  ).setMessageConverters(converter).build()
  val githubMvc = MockMvcBuilders.standaloneSetup(
    GithubTrendingController(githubTrendingService, githubSemanticSearchService),
    GithubTrendingSummaryController(githubTrendingService)
  ).setMessageConverters(converter).build()

  val publishedAt = LocalDateTime.of(2024, 1, 2, 3, 4, 5)

  test("GET /api/v1/search/instant - JSON 계약") {
    every { searchService.instantSearch("kotlin") } returns InstantSearchResponse(
      query = "kotlin",
      companies = listOf(CompanyMatchDto("c1", "Toss", "toss.png", 10, 3, "<mark>Toss</mark>")),
      posts = listOf(
        PostMatchDto("p1", "title", "Toss", "toss.png", 7, publishedAt, "<mark>title</mark>", setOf(Category.BackEnd))
      )
    )

    searchMvc.get("/api/v1/search/instant") { param("query", "kotlin") }
      .andExpect {
        status { isOk() }
        content {
          json(
            """
            {"query":"kotlin",
             "companies":[{"id":"c1","name":"Toss","logoImageName":"toss.png","postCount":10,"matchedPostCount":3,"highlightedName":"<mark>Toss</mark>"}],
             "posts":[{"id":"p1","title":"title","companyName":"Toss","companyLogo":"toss.png","viewCount":7,"publishedAt":"2024-01-02T03:04:05","highlightedTitle":"<mark>title</mark>","categories":["BackEnd"]}],
             "isEmpty":false}
            """.trimIndent(),
            true
          )
        }
      }
  }

  test("GET /api/v1/search - JSON 계약") {
    every { searchService.fullSearch(any()) } returns FullSearchResponse(
      query = "kotlin",
      posts = listOf(
        PostSearchResultDto(
          "p1", "title", "preview", "https://x/p1", null, "c1", "Toss", "toss.png", 7, publishedAt,
          true, setOf(Category.BackEnd), 1.5
        )
      ),
      totalCount = 1, currentPage = 0, totalPages = 1, hasNext = false
    )

    searchMvc.get("/api/v1/search") { param("query", "kotlin") }
      .andExpect {
        status { isOk() }
        content {
          json(
            """
            {"query":"kotlin",
             "posts":[{"id":"p1","title":"title","preview":"preview","url":"https://x/p1","thumbnail":null,"companyId":"c1","companyName":"Toss","companyLogo":"toss.png","viewCount":7,"publishedAt":"2024-01-02T03:04:05","isSummary":true,"categories":["BackEnd"],"relevanceScore":1.5}],
             "totalCount":1,"currentPage":0,"totalPages":1,"hasNext":false}
            """.trimIndent(),
            true
          )
        }
      }
  }

  test("GET /api/v1/search/semantic - JSON 계약") {
    val company = CompanyDto(
      id = "c1", name = "Toss", blogUrl = "https://toss.tech", logoImageName = "toss.png",
      rssSupported = true, totalViewCount = 1000, postCount = 50
    )
    val post = PostDto(
      id = "p1", title = "title", preview = "preview", url = "https://x/p1", content = "content",
      publishedAt = publishedAt, thumbnail = null, company = company, viewCount = 7,
      categories = setOf(Category.BackEnd), isSummary = true, isEmbedding = true
    )
    every { semanticSearchService.search("kotlin", 10, null) } returns listOf(SemanticSearchResult(post, 0.9, 1))

    searchMvc.get("/api/v1/search/semantic") { param("query", "kotlin") }
      .andExpect {
        status { isOk() }
        content {
          json(
            """
            {"query":"kotlin",
             "results":[{"post":{"id":"p1","title":"title","preview":"preview","url":"https://x/p1","thumbnail":null,"companyId":"c1","companyName":"Toss","companyLogo":"toss.png","viewCount":7,"publishedAt":"2024-01-02T03:04:05","isSummary":true,"categories":["BackEnd"],"relevanceScore":0.0},
                         "similarityScore":0.9,"rank":1}],
             "totalReturned":1}
            """.trimIndent(),
            false
          )
        }
      }
  }

  test("GET /api/v1/github/search - JSON 계약") {
    every { githubSemanticSearchService.search("kotlin", 10) } returns listOf(
      GithubSemanticSearchResult(
        fullName = "o/r", repoName = "r", description = "d", readmeSummary = "s", primaryLanguage = "Kotlin",
        starCount = 5, ownerName = "o", ownerAvatarUrl = null, topics = listOf("a"), htmlUrl = "https://g/o/r",
        similarityScore = 0.8, rank = 1
      )
    )

    githubMvc.get("/api/v1/github/search") { param("query", "kotlin") }
      .andExpect {
        status { isOk() }
        content {
          json(
            """
            {"query":"kotlin",
             "results":[{"fullName":"o/r","repoName":"r","description":"d","readmeSummary":"s","primaryLanguage":"Kotlin","starCount":5,"ownerName":"o","ownerAvatarUrl":null,"topics":["a"],"htmlUrl":"https://g/o/r","similarityScore":0.8,"rank":1}],
             "totalReturned":1}
            """.trimIndent(),
            false
          )
        }
      }
  }

  test("GET /api/v1/github/trending/summary - JSON 계약") {
    every { githubTrendingService.getSummary(null) } returns GithubSummaryDto(100L, 50000L)

    githubMvc.get("/api/v1/github/trending/summary")
      .andExpect {
        status { isOk() }
        content { json("""{"totalRepositories":100,"totalStars":50000}""", true) }
      }
  }
})
