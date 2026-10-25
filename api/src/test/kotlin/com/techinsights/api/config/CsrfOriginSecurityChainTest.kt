package com.techinsights.api.config

import com.techinsights.api.auth.AuthController
import com.techinsights.api.auth.AuthCookies
import com.techinsights.api.auth.CookieOAuth2AuthorizationRequestRepository
import com.techinsights.api.auth.CustomOAuth2UserService
import com.techinsights.api.auth.JwtAuthenticationFilter
import com.techinsights.api.auth.OAuth2FailureHandler
import com.techinsights.api.auth.OAuth2SuccessHandler
import com.techinsights.api.auth.TokenService
import com.techinsights.api.props.AuthProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Runs the REAL SecurityFilterChain (SecurityConfig: CorsFilter from the real CORS config, real CsrfOriginFilter) against
 * the cookie-authenticated POST /api/v1/auth/refresh. The controller itself answers 401 INVALID_TOKEN when no refresh
 * cookie is sent, which is how "the request got past the CSRF layers" shows up here.
 *
 * Which layer rejects what:
 *  - a foreign Origin is rejected by Spring Security's CorsFilter (403, plain-text body "Invalid CORS request" — NOT
 *    problem+json), before CsrfOriginFilter runs;
 *  - CsrfOriginFilter adds the Referer fallback for requests without Origin (403 problem+json CSRF_ORIGIN_NOT_ALLOWED).
 */
@WebMvcTest(
    controllers = [AuthController::class],
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
@Import(SecurityConfig::class, CsrfOriginSecurityChainTest.TestBeans::class)
@TestPropertySource(
    properties = [
        "auth.jwt.secret-key=dGhpcy1pcy1hLXZlcnktc2VjdXJlLXNlY3JldC1rZXkhIQ==",
        // application-local.yml would otherwise re-bind CorsProperties to localhost origins
        "cors.allowed-origins=https://www.techinsights.shop,https://techinsights.shop"
    ]
)
class CsrfOriginSecurityChainTest(
    @Autowired val mockMvc: MockMvc
) : FunSpec() {
    override fun extensions() = listOf(SpringExtension)

    // AuthProperties is constructor-bound from auth.jwt.secret-key (see @TestPropertySource).
    @TestConfiguration
    @EnableConfigurationProperties(AuthProperties::class)
    class TestBeans {
        @Bean
        fun corsProperties() = CorsProperties(
            allowedOrigins = listOf("https://www.techinsights.shop", "https://techinsights.shop")
        )

        @Bean
        fun authCookies(authProperties: AuthProperties) = AuthCookies(authProperties)
    }

    @MockitoBean private lateinit var tokenService: TokenService
    @MockitoBean private lateinit var customOAuth2UserService: CustomOAuth2UserService
    @MockitoBean private lateinit var oAuth2SuccessHandler: OAuth2SuccessHandler
    @MockitoBean private lateinit var oAuth2FailureHandler: OAuth2FailureHandler
    @MockitoBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockitoBean private lateinit var cookieOAuth2AuthorizationRequestRepository: CookieOAuth2AuthorizationRequestRepository

    init {
        beforeTest {
            doAnswer { invocation ->
                invocation.getArgument<FilterChain>(2)
                    .doFilter(invocation.getArgument<ServletRequest>(0), invocation.getArgument<ServletResponse>(1))
                null
            }.`when`(jwtAuthenticationFilter).doFilter(
                any(ServletRequest::class.java),
                any(ServletResponse::class.java),
                any(FilterChain::class.java)
            )
        }

        val refresh = "/api/v1/auth/refresh"

        test("같은 사이트의 형제 서브도메인 Origin 의 POST 는 거부된다 (CorsFilter, problem+json 아님)") {
            val result = mockMvc.perform(post(refresh).header("Origin", "https://evil.techinsights.shop"))
                .andExpect(status().isForbidden)
                .andReturn()

            result.response.contentAsString shouldContain "Invalid CORS request"
            (result.response.contentType ?: "").contains("problem+json") shouldBe false
        }

        test("허용된 Origin 의 POST 는 CSRF 계층을 통과한다 (컨트롤러의 401 INVALID_TOKEN 이 나온다)") {
            mockMvc.perform(post(refresh).header("Origin", "https://www.techinsights.shop"))
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
        }

        test("Origin 없이 허용되지 않은 Referer 만 있는 POST 는 CsrfOriginFilter 가 403 problem+json 으로 막는다") {
            mockMvc.perform(post(refresh).header("Referer", "https://evil.techinsights.shop/page"))
                .andExpect(status().isForbidden)
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("CSRF_ORIGIN_NOT_ALLOWED"))
        }

        test("Origin 없이 허용된 Referer 만 있는 POST 는 통과한다") {
            mockMvc.perform(post(refresh).header("Referer", "https://www.techinsights.shop/posts/1"))
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
        }

        test("Origin 도 Referer 도 없는 POST 는 통과한다 (브라우저가 아닌 클라이언트)") {
            mockMvc.perform(post(refresh))
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
        }
    }
}
