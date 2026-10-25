package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.slf4j.LoggerFactory
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
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

    val handler = OAuth2FailureHandler(authProperties, AuthCookies(authProperties))

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
        val cookieHandler = OAuth2FailureHandler(cookieProperties, AuthCookies(cookieProperties))

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

    context("로깅") {
        val logger = LoggerFactory.getLogger(OAuth2FailureHandler::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>()

        beforeTest {
            appender.list.clear()
            appender.start()
            logger.addAppender(appender)
        }
        afterTest { logger.detachAppender(appender) }

        test("오류 코드와 예외 클래스만 기록하고 제공자 응답 설명이나 예외 메시지는 기록하지 않는다") {
            val exception = OAuth2AuthenticationException(
                OAuth2Error("invalid_grant", "provider said: user@example.com code=4/0AbC", null),
                "internal detail user@example.com"
            )

            handler.onAuthenticationFailure(MockHttpServletRequest(), MockHttpServletResponse(), exception)

            val message = appender.list.single().formattedMessage
            message.contains("invalid_grant") shouldBe true
            message.contains("OAuth2AuthenticationException") shouldBe true
            message.contains("user@example.com") shouldBe false
            message.contains("4/0AbC") shouldBe false
        }

        test("OAuth2 가 아닌 인증 예외도 메시지 없이 클래스만 기록한다") {
            handler.onAuthenticationFailure(
                MockHttpServletRequest(), MockHttpServletResponse(), BadCredentialsException("secret detail")
            )

            val message = appender.list.single().formattedMessage
            message.contains("BadCredentialsException") shouldBe true
            message.contains("secret detail") shouldBe false
        }
    }
})
