package com.techinsights.api.config

import com.techinsights.api.service.auth.CustomOAuth2UserService
import com.techinsights.api.service.auth.OAuth2SuccessHandler
import com.techinsights.api.util.auth.JwtAuthenticationFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.springframework.mock.web.MockHttpServletRequest

class SecurityConfigIntegrationTest : FunSpec({
    val customOAuth2UserService = mockk<CustomOAuth2UserService>(relaxed = true)
    val oAuth2SuccessHandler = mockk<OAuth2SuccessHandler>(relaxed = true)
    val jwtAuthenticationFilter = mockk<JwtAuthenticationFilter>(relaxed = true)
    val corsProperties = CorsProperties(allowedOrigins = listOf("http://localhost:3000"))

    val securityConfig = SecurityConfig(
        customOAuth2UserService = customOAuth2UserService,
        oAuth2SuccessHandler = oAuth2SuccessHandler,
        jwtAuthenticationFilter = jwtAuthenticationFilter,
        corsProperties = corsProperties
    )

    test("CORS 설정이 올바르게 구성되어야 한다") {
        val corsConfigSource = securityConfig.corsConfigurationSource()
        val corsConfig = corsConfigSource.getCorsConfiguration(MockHttpServletRequest().apply {
            requestURI = "/api/v1/posts"
        })

        corsConfig?.allowedOrigins shouldBe listOf("http://localhost:3000")
        corsConfig?.allowedMethods shouldBe listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        corsConfig?.allowedHeaders shouldBe listOf("*")
        corsConfig?.allowCredentials shouldBe true
        corsConfig?.maxAge shouldBe 3600L
    }

    test("CORS preflight 요청을 올바르게 처리해야 한다") {
        val corsConfigSource = securityConfig.corsConfigurationSource()
        val request = MockHttpServletRequest().apply {
            method = "OPTIONS"
            requestURI = "/api/v1/posts"
            addHeader("Origin", "http://localhost:3000")
            addHeader("Access-Control-Request-Method", "POST")
        }
        val corsConfig = corsConfigSource.getCorsConfiguration(request)

        corsConfig?.allowedMethods?.contains("OPTIONS") shouldBe true
        corsConfig?.allowedOrigins?.contains("http://localhost:3000") shouldBe true
    }
})
