package com.techinsights.api.search

import com.techinsights.api.exception.GlobalExceptionHandler
import com.techinsights.domain.config.search.SemanticSearchProperties
import com.techinsights.domain.service.search.SearchService
import com.techinsights.domain.service.search.SemanticSearchService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    controllers = [SearchController::class],
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        OAuth2ClientWebSecurityAutoConfiguration::class,
        OAuth2ResourceServerAutoConfiguration::class
    ],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = [
                "com\\.techinsights\\.api\\.config\\..*",
                "com\\.techinsights\\.api\\.auth\\..*",
                "com\\.techinsights\\.api\\.aid\\..*"
            ]
        )
    ]
)
@Import(SearchControllerValidationIntegrationTest.CoroutineTestConfig::class, GlobalExceptionHandler::class)
class SearchControllerValidationIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var searchService: SearchService

    @MockitoBean
    private lateinit var semanticSearchService: SemanticSearchService

    @Test
    fun `fullSearch should return 400 when page is negative`() {
        val mvcResult = mockMvc.perform(
            get("/api/v1/search")
                .param("query", "kotlin")
                .param("page", "-1")
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isBadRequest)

        verifyNoInteractions(searchService)
    }

    @Test
    fun `fullSearch should return 400 when size is zero`() {
        val mvcResult = mockMvc.perform(
            get("/api/v1/search")
                .param("query", "kotlin")
                .param("size", "0")
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isBadRequest)

        verifyNoInteractions(searchService)
    }

    @Test
    fun `fullSearch should return 400 when size exceeds max`() {
        val mvcResult = mockMvc.perform(
            get("/api/v1/search")
                .param("query", "kotlin")
                .param("size", "101")
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isBadRequest)

        verifyNoInteractions(searchService)
    }

    @Test
    fun `fullSearch should return 400 when query is blank`() {
        val mvcResult = mockMvc.perform(
            get("/api/v1/search")
                .param("query", " ")
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isBadRequest)

        verifyNoInteractions(searchService)
    }

    @Test
    fun `instantSearch should return 400 when query is blank`() {
        val mvcResult = mockMvc.perform(
            get("/api/v1/search/instant")
                .param("query", " ")
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isBadRequest)

        verifyNoInteractions(searchService)
    }

    @Test
    fun `semanticSearch should return 400 when query is blank`() {
        val mvcResult = mockMvc.perform(
            get("/api/v1/search/semantic")
                .param("query", " ")
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isBadRequest)

        verifyNoInteractions(semanticSearchService)
    }

    @Test
    fun `semanticSearch should return 400 when size exceeds max (greater than 20)`() {
        val mvcResult = mockMvc.perform(
            get("/api/v1/search/semantic")
                .param("query", "kotlin")
                .param("size", "21")
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isBadRequest)

        verifyNoInteractions(semanticSearchService)
    }

    @Test
    fun `semanticSearch should return 400 when size is less than 1`() {
        val mvcResult = mockMvc.perform(
            get("/api/v1/search/semantic")
                .param("query", "kotlin")
                .param("size", "0")
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isBadRequest)

        verifyNoInteractions(semanticSearchService)
    }

    @Test
    fun `semanticSearch should return 400 when query exceeds max length`() {
        val tooLongQuery = "a".repeat(501) // MAX_QUERY_LENGTH = 500

        val mvcResult = mockMvc.perform(
            get("/api/v1/search/semantic")
                .param("query", tooLongQuery)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isBadRequest)

        verifyNoInteractions(semanticSearchService)
    }

    @TestConfiguration
    class CoroutineTestConfig {
        @Bean("ioDispatcher")
        fun ioDispatcher(): CoroutineDispatcher = Dispatchers.Unconfined

        @Bean
        fun semanticSearchProperties(): SemanticSearchProperties =
            SemanticSearchProperties(defaultSize = 10, maxSize = 20)
    }
}
