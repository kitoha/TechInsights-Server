package com.techinsights.api.aid

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.mock.web.MockHttpServletRequest

class DefaultCookiePolicyTest : FunSpec({
    val policy = DefaultCookiePolicy()

    fun defaultProps() = AidProperties().apply {
        cookie.sameSiteDefault = "Lax"
        cookie.secureMode = AidProperties.SecureMode.AUTO
    }

    test("Origin 헤더가 없으면 sameSiteDefault를 반환한다") {
        val request = MockHttpServletRequest().apply {
            serverName = "example.com"
        }

        policy.sameSite(request, defaultProps()) shouldBe "Lax"
    }

    test("Origin 헤더가 비정상이면 sameSiteDefault를 반환한다") {
        val request = MockHttpServletRequest().apply {
            serverName = "example.com"
            addHeader("Origin", "not-a-valid-origin")
        }

        policy.sameSite(request, defaultProps()) shouldBe "Lax"
    }

    test("크로스 사이트이면서 secure=true면 None을 반환한다") {
        val request = MockHttpServletRequest().apply {
            serverName = "example.com"
            addHeader("Origin", "https://other.com")
            addHeader("X-Forwarded-Proto", "https")
        }

        policy.sameSite(request, defaultProps()) shouldBe "None"
    }

    test("크로스 사이트여도 secure=false면 sameSiteDefault를 반환한다") {
        val request = MockHttpServletRequest().apply {
            serverName = "example.com"
            addHeader("Origin", "https://other.com")
        }

        policy.sameSite(request, defaultProps()) shouldBe "Lax"
    }

    test("contains 오탐 케이스(evil-example.com)는 크로스 사이트로 판단한다") {
        val request = MockHttpServletRequest().apply {
            serverName = "example.com"
            addHeader("Origin", "https://evil-example.com")
            addHeader("X-Forwarded-Proto", "https")
        }

        policy.sameSite(request, defaultProps()) shouldBe "None"
    }

    test("동일 사이트면 sameSiteDefault를 반환한다") {
        val request = MockHttpServletRequest().apply {
            serverName = "api.example.com"
            addHeader("Origin", "https://app.example.com")
        }

        policy.sameSite(request, defaultProps()) shouldBe "Lax"
    }
})
