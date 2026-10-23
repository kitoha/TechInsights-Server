package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.mock.web.MockHttpServletResponse
import java.util.Base64

class AuthCookiesTest : FunSpec({

    fun props(
        secure: Boolean = true,
        sameSite: String = "Lax",
        domain: String? = null
    ) = AuthProperties(
        jwt = AuthProperties.Jwt(
            secretKey = Base64.getEncoder().encodeToString("this-is-a-very-secure-secret-key!!".toByteArray()),
            accessTokenCookieName = "ti_at",
            refreshTokenCookieName = "ti_rt",
            cookieSecure = secure,
            cookieSameSite = sameSite,
            cookieDomain = domain
        )
    )

    test("add 는 path / HttpOnly / Secure / SameSite / Max-Age / Domain 속성을 설정한다") {
        val response = MockHttpServletResponse()

        AuthCookies(props(domain = "techinsights.shop")).add(response, "ti_at", "token-value", 1800)

        val header = response.getHeader("Set-Cookie")!!
        listOf("ti_at=token-value", "Path=/", "HttpOnly", "Secure", "SameSite=Lax", "Max-Age=1800", "Domain=techinsights.shop")
            .forEach { header.contains(it) shouldBe true }
    }

    test("clear 는 add 와 같은 속성(path/domain/secure/samesite/httponly)으로 Max-Age=0 쿠키를 보낸다") {
        val properties = props(secure = true, sameSite = "None", domain = "techinsights.shop")
        val cookies = AuthCookies(properties)
        val addResponse = MockHttpServletResponse()
        val clearResponse = MockHttpServletResponse()

        cookies.add(addResponse, "ti_at", "token-value", 1800)
        cookies.clear(clearResponse, "ti_at")

        fun attributes(header: String) = header.split(";").map { it.trim() }
            .filterNot { it.startsWith("ti_at=") || it.startsWith("Max-Age") || it.startsWith("Expires") }
            .toSet()
        attributes(clearResponse.getHeader("Set-Cookie")!!) shouldBe attributes(addResponse.getHeader("Set-Cookie")!!)
        clearResponse.getHeader("Set-Cookie")!!.contains("Max-Age=0") shouldBe true
        clearResponse.getHeader("Set-Cookie")!!.startsWith("ti_at=;") shouldBe true
    }

    test("domain 이 비어 있으면 Domain 속성을 넣지 않는다") {
        val response = MockHttpServletResponse()

        AuthCookies(props(domain = " ")).add(response, "ti_at", "v", 10)
        AuthCookies(props(domain = null)).clear(response, "ti_at")

        response.getHeaders("Set-Cookie").none { it.contains("Domain=") } shouldBe true
    }

    test("clearAuthCookies 는 access / refresh 쿠키를 모두 만료시킨다") {
        val response = MockHttpServletResponse()

        AuthCookies(props()).clearAuthCookies(response)

        val headers = response.getHeaders("Set-Cookie")
        headers.size shouldBe 2
        headers.any { it.startsWith("ti_at=;") && it.contains("Max-Age=0") } shouldBe true
        headers.any { it.startsWith("ti_rt=;") && it.contains("Max-Age=0") } shouldBe true
    }
})
