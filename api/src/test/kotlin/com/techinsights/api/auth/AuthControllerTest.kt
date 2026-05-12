package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.time.Duration
import java.util.Base64

class AuthControllerTest : FunSpec({
    val tokenService = mockk<TokenService>()
    val authProperties = AuthProperties(
        jwt = AuthProperties.Jwt(
            secretKey = Base64.getEncoder().encodeToString("this-is-a-very-secure-secret-key!!".toByteArray()),
            accessTokenExpiration = Duration.ofMinutes(15),
            refreshTokenExpiration = Duration.ofDays(30),
            cookieSecure = true
        )
    )
    val controller = AuthController(tokenService, authProperties)

    beforeTest {
        clearMocks(tokenService)
    }

    test("/refresh 호출 시 X-Device-Id 헤더가 있으면 해당 deviceId로 RTR이 작동해야 한다") {
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>()
        val oldRt = "old-rt"
        val cookies = arrayOf(Cookie(authProperties.jwt.refreshTokenCookieName, oldRt))

        every { request.cookies } returns cookies
        every { request.getHeader("X-Device-Id") } returns "device-uuid-123"
        every { tokenService.refresh(oldRt, "device-uuid-123") } returns TokenResponse("new-at", "new-rt")
        every { response.addHeader(any(), any()) } just Runs

        val result = controller.refresh(request, response)

        result.statusCode.value() shouldBe 200
        val atName = authProperties.jwt.accessTokenCookieName
        val rtName = authProperties.jwt.refreshTokenCookieName
        verify { response.addHeader("Set-Cookie", match { it.contains("$atName=new-at") && it.contains("Secure") }) }
        verify { response.addHeader("Set-Cookie", match { it.contains("$rtName=new-rt") && it.contains("Secure") }) }
    }

    test("X-Device-Id 헤더가 없으면 null deviceId로 refresh해야 한다 (User-Agent fallback 없음)") {
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>()
        val cookies = arrayOf(Cookie(authProperties.jwt.refreshTokenCookieName, "old-rt"))

        every { request.cookies } returns cookies
        every { request.getHeader("X-Device-Id") } returns null
        every { tokenService.refresh("old-rt", null) } returns TokenResponse("new-at", "new-rt")
        every { response.addHeader(any(), any()) } just Runs

        val result = controller.refresh(request, response)

        result.statusCode.value() shouldBe 200
        verify { tokenService.refresh("old-rt", null) }
    }

    test("/logout 호출 시 세션이 무효화되고 쿠키가 삭제되어야 한다") {
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>()
        val rt = "valid-rt"
        val cookies = arrayOf(Cookie(authProperties.jwt.refreshTokenCookieName, rt))

        every { request.cookies } returns cookies
        every { tokenService.revoke(rt) } just Runs
        every { response.addHeader(any(), any()) } just Runs

        val result = controller.logout(request, response)

        result.statusCode.value() shouldBe 200
        verify { tokenService.revoke(rt) }
        val atName = authProperties.jwt.accessTokenCookieName
        val rtName = authProperties.jwt.refreshTokenCookieName
        verify { response.addHeader("Set-Cookie", match { it.contains("$atName=") && it.contains("Max-Age=0") && it.contains("Secure") }) }
        verify { response.addHeader("Set-Cookie", match { it.contains("$rtName=") && it.contains("Max-Age=0") && it.contains("Secure") }) }
    }

    test("cookieDomain이 설정되면 refresh가 발급하는 access/refresh 쿠키에 Domain 속성이 포함되어야 한다") {
        // given
        val authPropertiesWithDomain = AuthProperties(
            jwt = AuthProperties.Jwt(
                secretKey = Base64.getEncoder().encodeToString("this-is-a-very-secure-secret-key!!".toByteArray()),
                cookieDomain = "techinsights.shop"
            )
        )
        val controllerWithDomain = AuthController(tokenService, authPropertiesWithDomain)
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>()
        val rtName = authPropertiesWithDomain.jwt.refreshTokenCookieName
        val cookies = arrayOf(Cookie(rtName, "old-rt"))
        val setCookieHeaders = mutableListOf<String>()

        every { request.cookies } returns cookies
        every { request.getHeader("X-Device-Id") } returns null
        every { tokenService.refresh("old-rt", null) } returns TokenResponse("new-at", "new-rt")
        every { response.addHeader("Set-Cookie", capture(setCookieHeaders)) } just Runs

        // when
        controllerWithDomain.refresh(request, response)

        // then
        setCookieHeaders.count { it.contains("Domain=techinsights.shop") } shouldBe 2
    }

    test("cookieDomain이 설정되면 logout이 비우는 쿠키에도 Domain 속성이 포함되어야 한다") {
        // given
        val authPropertiesWithDomain = AuthProperties(
            jwt = AuthProperties.Jwt(
                secretKey = Base64.getEncoder().encodeToString("this-is-a-very-secure-secret-key!!".toByteArray()),
                cookieDomain = "techinsights.shop"
            )
        )
        val controllerWithDomain = AuthController(tokenService, authPropertiesWithDomain)
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>()
        val rtName = authPropertiesWithDomain.jwt.refreshTokenCookieName
        val cookies = arrayOf(Cookie(rtName, "rt"))
        val setCookieHeaders = mutableListOf<String>()

        every { request.cookies } returns cookies
        every { tokenService.revoke("rt") } just Runs
        every { response.addHeader("Set-Cookie", capture(setCookieHeaders)) } just Runs

        // when
        controllerWithDomain.logout(request, response)

        // then
        setCookieHeaders.count { it.contains("Domain=techinsights.shop") && it.contains("Max-Age=0") } shouldBe 2
    }
})
