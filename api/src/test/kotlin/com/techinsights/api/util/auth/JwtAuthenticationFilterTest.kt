package com.techinsights.api.util.auth

import com.techinsights.api.props.AuthProperties
import com.techinsights.domain.enums.UserRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import io.jsonwebtoken.Claims

class JwtAuthenticationFilterTest : FunSpec({
    val jwtPlugin = mockk<JwtPlugin>()
    val authProperties = AuthProperties(
        jwt = AuthProperties.Jwt(
            secretKey = "this-is-a-very-secure-secret-key-for-testing-purposes-only",
            accessTokenCookieName = "at"
        )
    )
    val filter = JwtAuthenticationFilter(jwtPlugin, authProperties)

    beforeTest {
        SecurityContextHolder.clearContext()
        clearMocks(jwtPlugin)
    }

    test("유효한 Access Token 쿠키가 있으면 SecurityContext에 인증 정보가 설정되어야 한다") {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mockk<FilterChain>()
        
        request.setCookies(Cookie("at", "valid-at"))
        
        val claims = mockk<Claims>()
        every { claims.get("userId", Long::class.javaObjectType) } returns 1L
        every { claims.get("email", String::class.java) } returns "test@example.com"
        every { claims.get("role", String::class.java) } returns UserRole.USER.name
        every { claims.subject } returns "1"

        every { jwtPlugin.validateToken("valid-at") } returns claims
        every { chain.doFilter(any(), any()) } just Runs

        // when
        filter.doFilter(request, response, chain)

        // then
        val auth = SecurityContextHolder.getContext().authentication
        auth shouldNotBe null
        (auth?.principal as CustomUserDetails).userId shouldBe 1L
        verify { chain.doFilter(any(), any()) }
    }

    test("Access Token 쿠키가 없으면 인증 정보를 설정하지 않아야 한다") {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mockk<FilterChain>()

        every { chain.doFilter(any(), any()) } just Runs

        // when
        filter.doFilter(request, response, chain)

        // then
        SecurityContextHolder.getContext().authentication shouldBe null
        verify { chain.doFilter(any(), any()) }
    }
})
