package com.techinsights.api.config

import com.techinsights.api.auth.CustomOAuth2UserService
import com.techinsights.api.auth.CookieOAuth2AuthorizationRequestRepository
import com.techinsights.api.auth.JwtAuthenticationFilter
import com.techinsights.api.auth.OAuth2FailureHandler
import com.techinsights.api.auth.OAuth2SuccessHandler
import io.kotest.core.spec.style.FunSpec
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.springframework.mock.web.MockHttpServletRequest

class SecurityConfigIntegrationTest : FunSpec({
    val customOAuth2UserService = mockk<CustomOAuth2UserService>(relaxed = true)
    val oAuth2SuccessHandler = mockk<OAuth2SuccessHandler>(relaxed = true)
    val oAuth2FailureHandler = mockk<OAuth2FailureHandler>(relaxed = true)
    val jwtAuthenticationFilter = mockk<JwtAuthenticationFilter>(relaxed = true)
    val clientRegistrationRepository = mockk<ClientRegistrationRepository>(relaxed = true)
    val cookieOAuth2AuthorizationRequestRepository = mockk<CookieOAuth2AuthorizationRequestRepository>(relaxed = true)
    val corsProperties = CorsProperties(allowedOrigins = listOf("http://localhost:3000"))

    val securityConfig = SecurityConfig(
        customOAuth2UserService = customOAuth2UserService,
        oAuth2SuccessHandler = oAuth2SuccessHandler,
        oAuth2FailureHandler = oAuth2FailureHandler,
        jwtAuthenticationFilter = jwtAuthenticationFilter,
        corsProperties = corsProperties,
        clientRegistrationRepository = clientRegistrationRepository,
        cookieOAuth2AuthorizationRequestRepository = cookieOAuth2AuthorizationRequestRepository
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
