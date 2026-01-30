package com.techinsights.api.service.auth

import com.techinsights.api.props.AuthProperties
import com.techinsights.api.response.auth.TokenResponse
import com.techinsights.api.util.auth.CustomUserDetails
import com.techinsights.domain.enums.UserRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Duration
import io.kotest.matchers.collections.shouldNotBeEmpty

class OAuth2SuccessHandlerTest : FunSpec({
    val tokenService = mockk<TokenService>()
    val authProperties = AuthProperties(
        jwt = AuthProperties.Jwt(
            secretKey = "this-is-a-very-secure-secret-key-for-testing-purposes-only",
            accessTokenExpiration = Duration.ofMinutes(15),
            refreshTokenExpiration = Duration.ofDays(30),
            cookieSecure = true
        ),
        oauth2 = AuthProperties.OAuth2(successRedirectUri = "/dashboard")
    )
    val handler = OAuth2SuccessHandler(tokenService, authProperties)

    test("성공 시 토큰을 쿠키에 담고 리다이렉트해야 한다") {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val auth = mockk<org.springframework.security.core.Authentication>()
        val principal = CustomUserDetails(1L, "test@example.com", UserRole.USER, emptyMap())

        every { auth.principal } returns principal
        request.addHeader("User-Agent", "device")
        every { tokenService.issueTokens(1L, "test@example.com", UserRole.USER, "device") } returns TokenResponse("at", "rt")

        // when
        handler.onAuthenticationSuccess(request, response, auth)

        // then
        val cookies = response.getHeaderValues("Set-Cookie")
        cookies.shouldNotBeEmpty()
        cookies.any { it.toString().contains("at=at") && it.toString().contains("Secure") } shouldBe true
        response.redirectedUrl shouldBe "/dashboard"
    }
})
