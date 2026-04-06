package com.techinsights.api.config

import com.techinsights.api.auth.CustomOAuth2UserService
import com.techinsights.api.auth.JwtAuthenticationFilter
import com.techinsights.api.auth.OAuth2SuccessHandler
import com.techinsights.api.company.CompanyController
import com.techinsights.domain.service.company.CompanyService
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    controllers = [CompanyController::class],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = [
                "com\\.techinsights\\.api\\.config\\..*",
                "com\\.techinsights\\.api\\.aid\\..*"
            ]
        )
    ]
)
@Import(SecurityConfig::class, ActuatorSecurityTest.SecurityTestConfig::class)
class ActuatorSecurityTest(
    @Autowired val mockMvc: MockMvc
) : FunSpec() {
    override fun extensions() = listOf(SpringExtension)

    @TestConfiguration
    class SecurityTestConfig {
        @Bean
        fun corsProperties() = CorsProperties(allowedOrigins = listOf("http://localhost:3000"))
    }

    @MockitoBean
    private lateinit var companyService: CompanyService

    @MockitoBean
    private lateinit var customOAuth2UserService: CustomOAuth2UserService

    @MockitoBean
    private lateinit var oAuth2SuccessHandler: OAuth2SuccessHandler

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    init {
        beforeTest {
            doAnswer { invocation ->
                val request = invocation.getArgument<ServletRequest>(0)
                val response = invocation.getArgument<ServletResponse>(1)
                val chain = invocation.getArgument<FilterChain>(2)
                chain.doFilter(request, response)
                null
            }.`when`(jwtAuthenticationFilter).doFilter(
                any(ServletRequest::class.java),
                any(ServletResponse::class.java),
                any(FilterChain::class.java)
            )
        }

        test("GET /actuator/prometheus는 비인증 사용자에게 401을 반환해야 한다") {
            mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized)
        }

        test("GET /actuator/metrics는 비인증 사용자에게 401을 반환해야 한다") {
            mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized)
        }

        test("GET /actuator/info는 비인증 사용자에게 401을 반환해야 한다") {
            mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized)
        }

        test("GET /actuator/health는 비인증 사용자도 접근 가능해야 한다") {
            val result = mockMvc.perform(get("/actuator/health")).andReturn()
            assertThat(result.response.status).isNotEqualTo(401)
        }
    }
}
