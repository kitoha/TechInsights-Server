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

    test("삭제/비활성 계정 로그인 거부는 error=account_deactivated 로 리다이렉트해야 한다") {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val exception = OAuth2AuthenticationException(OAuth2Error("account_deactivated"))

        handler.onAuthenticationFailure(request, response, exception)

        response.redirectedUrl shouldBe "https://www.techinsights.shop/?error=account_deactivated"
    }

    context("인증 쿠키 삭제") {
        val cookieProperties = AuthProperties(
            jwt = AuthProperties.Jwt(
                secretKey = Base64.getEncoder().encodeToString("this-is-a-very-secure-secret-key!!".toByteArray()),
                accessTokenCookieName = "ti_at",
                refreshTokenCookieName = "ti_rt",
                cookieSecure = true,
                cookieSameSite = "Lax",
                cookieDomain = "techinsights.shop"
            ),
            oauth2 = AuthProperties.OAuth2(successRedirectUri = "https://www.techinsights.shop/")
        )
        val cookieHandler = OAuth2FailureHandler(cookieProperties)

        listOf("account_deactivated", "access_denied").forEach { errorCode ->
            test("$errorCode 실패 시 access/refresh 쿠키를 설정 때와 같은 속성으로 만료시킨다") {
                val response = MockHttpServletResponse()

                cookieHandler.onAuthenticationFailure(
                    MockHttpServletRequest(), response, OAuth2AuthenticationException(OAuth2Error(errorCode))
                )

                val setCookies = response.getHeaders("Set-Cookie")
                setCookies.size shouldBe 2
                listOf("ti_at", "ti_rt").forEach { name ->
                    val header = setCookies.single { it.startsWith("$name=;") }
                    listOf("Max-Age=0", "Path=/", "Domain=techinsights.shop", "Secure", "HttpOnly", "SameSite=Lax")
                        .forEach { attribute -> (header.contains(attribute)) shouldBe true }
                }
            }
        }

        test("cookieDomain 이 없으면 Domain 속성 없이 만료시킨다") {
            val response = MockHttpServletResponse()

            handler.onAuthenticationFailure(
                MockHttpServletRequest(), response, OAuth2AuthenticationException(OAuth2Error("access_denied"))
            )

            response.getHeaders("Set-Cookie").size shouldBe 2
            response.getHeaders("Set-Cookie").none { it.contains("Domain=") } shouldBe true
        }
    }
})
