package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import jakarta.servlet.http.Cookie
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import java.time.Duration
import java.util.Base64

class CookieOAuth2AuthorizationRequestRepositoryTest : FunSpec({

    val cookieName = "test_oauth2_auth_request"
    val authProperties = AuthProperties(
        jwt = AuthProperties.Jwt(
            secretKey = Base64.getEncoder().encodeToString("this-is-a-very-secure-secret-key!!".toByteArray())
        ),
        oauth2 = AuthProperties.OAuth2(
            authorizationRequestCookieName = cookieName,
            authorizationRequestCookieMaxAge = Duration.ofMinutes(3),
            authorizationRequestCookieSameSite = "Strict"
        )
    )
    val repository = CookieOAuth2AuthorizationRequestRepository(authProperties)

    fun authorizationRequest(): OAuth2AuthorizationRequest {
        return OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .clientId("client-id")
            .redirectUri("https://api.techinsights.shop/login/oauth2/code/google")
            .scopes(setOf("email", "profile"))
            .state("state|device-id")
            .build()
    }

    fun savedCookieValue(response: MockHttpServletResponse): String {
        return requireNotNull(response.getHeader("Set-Cookie"))
            .substringAfter("$cookieName=")
            .substringBefore(";")
    }

    test("OAuth2 authorization request를 쿠키에 저장하고 다시 로드해야 한다") {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val authorizationRequest = authorizationRequest()

        // when
        repository.saveAuthorizationRequest(authorizationRequest, request, response)
        val cookieValue = savedCookieValue(response)
        val callbackRequest = MockHttpServletRequest()
        callbackRequest.setCookies(Cookie(cookieName, cookieValue))
        val loaded = repository.loadAuthorizationRequest(callbackRequest)

        // then
        loaded?.state shouldBe "state|device-id"
        loaded?.redirectUri shouldBe "https://api.techinsights.shop/login/oauth2/code/google"
    }

    test("X-Forwarded-Proto가 https이면 Secure 쿠키로 저장해야 한다") {
        // given
        val request = MockHttpServletRequest()
        request.addHeader("X-Forwarded-Proto", "https")
        val response = MockHttpServletResponse()

        // when
        repository.saveAuthorizationRequest(authorizationRequest(), request, response)

        // then
        val setCookie = requireNotNull(response.getHeader("Set-Cookie"))
        setCookie.contains("Secure") shouldBe true
        setCookie.contains("SameSite=Strict") shouldBe true
        setCookie.contains("HttpOnly") shouldBe true
        setCookie.contains("Max-Age=180") shouldBe true
    }

    test("authorization request 제거 시 기존 요청을 반환하고 쿠키를 삭제해야 한다") {
        // given
        val saveResponse = MockHttpServletResponse()
        repository.saveAuthorizationRequest(authorizationRequest(), MockHttpServletRequest(), saveResponse)
        val cookieValue = savedCookieValue(saveResponse)
        val request = MockHttpServletRequest()
        request.setCookies(Cookie(cookieName, cookieValue))
        val response = MockHttpServletResponse()

        // when
        val removed = repository.removeAuthorizationRequest(request, response)

        // then
        removed?.state shouldBe "state|device-id"
        requireNotNull(response.getHeader("Set-Cookie")).contains("Max-Age=0") shouldBe true
    }

    test("서명이 변조된 쿠키는 로드하지 않아야 한다") {
        // given
        val saveResponse = MockHttpServletResponse()
        repository.saveAuthorizationRequest(authorizationRequest(), MockHttpServletRequest(), saveResponse)
        val cookieValue = savedCookieValue(saveResponse)
        val request = MockHttpServletRequest()
        request.setCookies(Cookie(cookieName, "$cookieValue-tampered"))

        // when
        val loaded = repository.loadAuthorizationRequest(request)

        // then
        loaded shouldBe null
    }
})
