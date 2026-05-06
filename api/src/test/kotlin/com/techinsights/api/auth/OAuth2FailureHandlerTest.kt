package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import java.time.Duration
import java.util.Base64

class OAuth2FailureHandlerTest : FunSpec({

    val authProperties = AuthProperties(
        jwt = AuthProperties.Jwt(
            secretKey = Base64.getEncoder().encodeToString("this-is-a-very-secure-secret-key!!".toByteArray()),
            accessTokenExpiration = Duration.ofMinutes(15),
            refreshTokenExpiration = Duration.ofDays(30),
            cookieSecure = true
        ),
        oauth2 = AuthProperties.OAuth2(successRedirectUri = "https://www.techinsights.shop/")
    )

    val handler = OAuth2FailureHandler(authProperties)

    test("OAuth2 인증 실패 시 프론트엔드로 error 파라미터와 함께 리다이렉트해야 한다") {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val exception = OAuth2AuthenticationException(OAuth2Error("access_denied"))

        // when
        handler.onAuthenticationFailure(request, response, exception)

        // then
        response.redirectedUrl shouldBe "https://www.techinsights.shop/?error=login_failed"
    }

    test("state 만료나 다른 OAuth2 오류도 동일하게 프론트엔드로 리다이렉트해야 한다") {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val exception = OAuth2AuthenticationException(OAuth2Error("invalid_request"))

        // when
        handler.onAuthenticationFailure(request, response, exception)

        // then
        response.redirectedUrl shouldBe "https://www.techinsights.shop/?error=login_failed"
    }
})
