package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import com.techinsights.domain.enums.UserRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.Authentication
import java.time.Duration
import java.util.Base64

class OAuth2SuccessHandlerTest : FunSpec({
    val tokenService = mockk<TokenService>()
    val authProperties = AuthProperties(
        jwt = AuthProperties.Jwt(
            secretKey = Base64.getEncoder().encodeToString("this-is-a-very-secure-secret-key!!".toByteArray()),
            accessTokenExpiration = Duration.ofMinutes(15),
            refreshTokenExpiration = Duration.ofDays(30),
            cookieSecure = true
        ),
        oauth2 = AuthProperties.OAuth2(successRedirectUri = "/dashboard")
    )
    val handler = OAuth2SuccessHandler(tokenService, authProperties)

    beforeTest { clearMocks(tokenService) }

    test("state에 deviceId가 있으면 해당 deviceId로 토큰을 발급해야 한다") {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val auth = mockk<Authentication>()
        val principal = CustomUserDetails(1L, "test@example.com", UserRole.USER, emptyMap())
        val state = "csrf-abc|device-uuid-123"

        every { auth.principal } returns principal
        request.addParameter("state", state)
        every { tokenService.issueTokens(1L, "test@example.com", UserRole.USER, "device-uuid-123") } returns TokenResponse("at", "rt")

        handler.onAuthenticationSuccess(request, response, auth)

        val cookies = response.getHeaderValues("Set-Cookie")
        cookies.shouldNotBeEmpty()
        response.redirectedUrl shouldBe "/dashboard"
        verify { tokenService.issueTokens(1L, "test@example.com", UserRole.USER, "device-uuid-123") }
    }

    test("state에 deviceId가 없으면 null deviceId로 토큰을 발급해야 한다") {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val auth = mockk<Authentication>()
        val principal = CustomUserDetails(1L, "test@example.com", UserRole.USER, emptyMap())

        every { auth.principal } returns principal
        request.addParameter("state", "csrf-abc")
        every { tokenService.issueTokens(1L, "test@example.com", UserRole.USER, null) } returns TokenResponse("at", "rt")

        handler.onAuthenticationSuccess(request, response, auth)

        verify { tokenService.issueTokens(1L, "test@example.com", UserRole.USER, null) }
    }

    test("User-Agent만 있고 state에 deviceId가 없으면 null로 처리해야 한다 (fallback 제거)") {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val auth = mockk<Authentication>()
        val principal = CustomUserDetails(1L, "test@example.com", UserRole.USER, emptyMap())

        every { auth.principal } returns principal
        request.addHeader("User-Agent", "Mozilla/5.0")
        request.addParameter("state", "csrf-abc")
        every { tokenService.issueTokens(1L, "test@example.com", UserRole.USER, null) } returns TokenResponse("at", "rt")

        handler.onAuthenticationSuccess(request, response, auth)

        verify { tokenService.issueTokens(1L, "test@example.com", UserRole.USER, null) }
        verify(exactly = 0) { tokenService.issueTokens(1L, "test@example.com", UserRole.USER, "Mozilla/5.0") }
    }
})
