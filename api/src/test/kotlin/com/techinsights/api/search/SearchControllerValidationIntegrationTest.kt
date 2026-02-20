package com.techinsights.api.search

import com.techinsights.api.exception.GlobalExceptionHandler
import com.techinsights.domain.service.search.SearchService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(SearchController::class)
@Import(SearchControllerValidationIntegrationTest.CoroutineTestConfig::class, GlobalExceptionHandler::class)
class SearchControllerValidationIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var searchService: SearchService

    @Test
    fun `fullSearch should return 400 when page is negative`() {
        mockMvc.get("/api/v1/search") {
            param("query", "kotlin")
            param("page", "-1")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
        }

        verifyNoInteractions(searchService)
    }

    @Test
    fun `fullSearch should return 400 when size is zero`() {
        mockMvc.get("/api/v1/search") {
            param("query", "kotlin")
            param("size", "0")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
        }

        verifyNoInteractions(searchService)
    }

    @Test
    fun `fullSearch should return 400 when size exceeds max`() {
        mockMvc.get("/api/v1/search") {
            param("query", "kotlin")
            param("size", "101")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
        }

        verifyNoInteractions(searchService)
    }

    @Test
    fun `fullSearch should return 400 when query is blank`() {
        mockMvc.get("/api/v1/search") {
            param("query", " ")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
        }

        verifyNoInteractions(searchService)
    }

    @Test
    fun `instantSearch should return 400 when query is blank`() {
        mockMvc.get("/api/v1/search/instant") {
            param("query", " ")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
        }

        verifyNoInteractions(searchService)
    }

    @TestConfiguration
    class CoroutineTestConfig {
        @Bean("ioDispatcher")
        fun ioDispatcher(): CoroutineDispatcher = Dispatchers.Unconfined
    }
}
