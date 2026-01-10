package com.techinsights.api.controller.search

import com.techinsights.domain.dto.search.FullSearchResponse
import com.techinsights.domain.dto.search.InstantSearchResponse
import com.techinsights.domain.dto.search.SearchRequest
import com.techinsights.domain.enums.search.SearchSortType
import com.techinsights.domain.service.search.SearchService
import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class SearchControllerTest : FunSpec() {

    private lateinit var mockMvc: MockMvc
    private val searchService = mockk<SearchService>()
    private val ioDispatcher = Dispatchers.IO

    init {
        beforeTest {
            clearAllMocks()

            val controller = SearchController(searchService, ioDispatcher)
            mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        }

        test("GET /api/v1/search/instant - should return instant search results") {
            // given
            val query = "kotlin"
            val response = InstantSearchResponse(
                query = query,
                companies = emptyList(),
                posts = emptyList()
            )

            coEvery { searchService.instantSearch(query) } returns response

            // when & then
            mockMvc.get("/api/v1/search/instant") {
                param("query", query)
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }

            coVerify(exactly = 1) { searchService.instantSearch(query) }
        }

        test("GET /api/v1/search/instant - should handle empty query") {
            // given
            val query = ""
            val response = InstantSearchResponse(
                query = query,
                companies = emptyList(),
                posts = emptyList()
            )

            coEvery { searchService.instantSearch(query) } returns response

            // when & then
            mockMvc.get("/api/v1/search/instant") {
                param("query", query)
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }

            coVerify(exactly = 1) { searchService.instantSearch(query) }
        }

        test("GET /api/v1/search - should return full search results with default parameters") {
            // given
            val query = "spring boot"
            val response = FullSearchResponse(
                query = query,
                posts = emptyList(),
                totalCount = 10,
                currentPage = 0,
                totalPages = 1,
                hasNext = false
            )

            coEvery {
                searchService.fullSearch(match {
                    it.query == query &&
                    it.page == 0 &&
                    it.size == 10 &&
                    it.sortBy == SearchSortType.RELEVANCE &&
                    it.companyId == null
                })
            } returns response

            // when & then
            mockMvc.get("/api/v1/search") {
                param("query", query)
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }

            coVerify(exactly = 1) {
                searchService.fullSearch(match {
                    it.query == query &&
                    it.page == 0 &&
                    it.size == 10 &&
                    it.sortBy == SearchSortType.RELEVANCE
                })
            }
        }

        test("GET /api/v1/search - should accept custom pagination parameters") {
            // given
            val query = "java"
            val page = 2
            val size = 20
            val response = FullSearchResponse(
                query = query,
                posts = emptyList(),
                totalCount = 100,
                currentPage = page,
                totalPages = 5,
                hasNext = true
            )

            coEvery {
                searchService.fullSearch(match {
                    it.query == query &&
                    it.page == page &&
                    it.size == size
                })
            } returns response

            // when & then
            mockMvc.get("/api/v1/search") {
                param("query", query)
                param("page", page.toString())
                param("size", size.toString())
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }

            coVerify(exactly = 1) {
                searchService.fullSearch(match {
                    it.page == page && it.size == size
                })
            }
        }

        test("GET /api/v1/search - should accept LATEST sort type") {
            // given
            val query = "kotlin"
            val sortBy = SearchSortType.LATEST
            val response = FullSearchResponse(
                query = query,
                posts = emptyList(),
                totalCount = 5,
                currentPage = 0,
                totalPages = 1,
                hasNext = false
            )

            coEvery {
                searchService.fullSearch(match {
                    it.query == query &&
                    it.sortBy == sortBy
                })
            } returns response

            // when & then
            mockMvc.get("/api/v1/search") {
                param("query", query)
                param("sortBy", sortBy.name)
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }

            coVerify(exactly = 1) {
                searchService.fullSearch(match {
                    it.sortBy == SearchSortType.LATEST
                })
            }
        }

        test("GET /api/v1/search - should filter by companyId when provided") {
            // given
            val query = "docker"
            val companyId = 123L
            val response = FullSearchResponse(
                query = query,
                posts = emptyList(),
                totalCount = 3,
                currentPage = 0,
                totalPages = 1,
                hasNext = false
            )

            coEvery {
                searchService.fullSearch(match {
                    it.query == query &&
                    it.companyId == companyId
                })
            } returns response

            // when & then
            mockMvc.get("/api/v1/search") {
                param("query", query)
                param("companyId", companyId.toString())
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }

            coVerify(exactly = 1) {
                searchService.fullSearch(match {
                    it.companyId == companyId
                })
            }
        }

        test("GET /api/v1/search - should handle all parameters together") {
            // given
            val query = "microservices"
            val page = 1
            val size = 15
            val sortBy = SearchSortType.LATEST
            val companyId = 456L
            val response = FullSearchResponse(
                query = query,
                posts = emptyList(),
                totalCount = 50,
                currentPage = page,
                totalPages = 4,
                hasNext = true
            )

            coEvery {
                searchService.fullSearch(match {
                    it.query == query &&
                    it.page == page &&
                    it.size == size &&
                    it.sortBy == sortBy &&
                    it.companyId == companyId
                })
            } returns response

            // when & then
            mockMvc.get("/api/v1/search") {
                param("query", query)
                param("page", page.toString())
                param("size", size.toString())
                param("sortBy", sortBy.name)
                param("companyId", companyId.toString())
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }

            coVerify(exactly = 1) {
                searchService.fullSearch(match {
                    it.query == query &&
                    it.page == page &&
                    it.size == size &&
                    it.sortBy == sortBy &&
                    it.companyId == companyId
                })
            }
        }

        test("GET /api/v1/search - should handle special characters in query") {
            // given
            val query = "C++ programming"
            val response = FullSearchResponse(
                query = query,
                posts = emptyList(),
                totalCount = 2,
                currentPage = 0,
                totalPages = 1,
                hasNext = false
            )

            coEvery {
                searchService.fullSearch(match { it.query == query })
            } returns response

            // when & then
            mockMvc.get("/api/v1/search") {
                param("query", query)
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }

            coVerify(exactly = 1) {
                searchService.fullSearch(match { it.query == query })
            }
        }

        test("GET /api/v1/search/instant - should handle Korean query") {
            // given
            val query = "스프링부트"
            val response = InstantSearchResponse(
                query = query,
                companies = emptyList(),
                posts = emptyList()
            )

            coEvery { searchService.instantSearch(query) } returns response

            // when & then
            mockMvc.get("/api/v1/search/instant") {
                param("query", query)
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }

            coVerify(exactly = 1) { searchService.instantSearch(query) }
        }

        test("GET /api/v1/search - should use RELEVANCE as default sort type") {
            // given
            val query = "test"
            val response = FullSearchResponse(
                query = query,
                posts = emptyList(),
                totalCount = 1,
                currentPage = 0,
                totalPages = 1,
                hasNext = false
            )

            coEvery {
                searchService.fullSearch(match {
                    it.sortBy == SearchSortType.RELEVANCE
                })
            } returns response

            // when & then
            mockMvc.get("/api/v1/search") {
                param("query", query)
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
            }

            coVerify(exactly = 1) {
                searchService.fullSearch(match {
                    it.sortBy == SearchSortType.RELEVANCE
                })
            }
        }
    }
}
