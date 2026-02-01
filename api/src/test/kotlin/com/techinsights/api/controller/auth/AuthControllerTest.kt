package com.techinsights.api.controller.auth

import com.techinsights.api.props.AuthProperties
import com.techinsights.api.response.auth.TokenResponse
import com.techinsights.api.service.auth.TokenService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.time.Duration

class AuthControllerTest : FunSpec({
    val tokenService = mockk<TokenService>()
    val authProperties = AuthProperties(
        jwt = AuthProperties.Jwt(
            secretKey = "this-is-a-very-secure-secret-key-for-testing-purposes-only",
            accessTokenExpiration = Duration.ofMinutes(15),
            refreshTokenExpiration = Duration.ofDays(30),
            cookieSecure = true
        )
    )
    val controller = AuthController(tokenService, authProperties)

    beforeTest {
        clearMocks(tokenService)
    }

    test("/refresh 호출 시 RTR이 작동하고 새 쿠키가 발급되어야 한다") {
        // given
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>()
        val oldRt = "old-rt"
        val cookies = arrayOf(Cookie("__Host-ti-rt", oldRt))

        every { request.cookies } returns cookies
        every { request.getHeader("X-Device-Id") } returns null
        every { request.getHeader("User-Agent") } returns "device-1"
        every { tokenService.refresh(oldRt, "device-1") } returns TokenResponse("new-at", "new-rt")
        every { response.addHeader(any(), any()) } just Runs

        // when
        val result = controller.refresh(request, response)

        // then
        result.statusCode.value() shouldBe 200
        verify { response.addHeader("Set-Cookie", match { it.contains("__Host-ti-at=new-at") && it.contains("Secure") }) }
        verify { response.addHeader("Set-Cookie", match { it.contains("__Host-ti-rt=new-rt") && it.contains("Secure") }) }
    }

    test("/logout 호출 시 세션이 무효화되고 쿠키가 삭제되어야 한다") {
        // given
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>()
        val rt = "valid-rt"
        val cookies = arrayOf(Cookie("__Host-ti-rt", rt))

        every { request.cookies } returns cookies
        every { tokenService.revoke(rt) } just Runs
        every { response.addHeader(any(), any()) } just Runs

        // when
        val result = controller.logout(request, response)

        // then
        result.statusCode.value() shouldBe 200
        verify { tokenService.revoke(rt) }
        verify { response.addHeader("Set-Cookie", match { it.contains("__Host-ti-at=") && it.contains("Max-Age=0") && it.contains("Secure") }) }
        verify { response.addHeader("Set-Cookie", match { it.contains("__Host-ti-rt=") && it.contains("Max-Age=0") && it.contains("Secure") }) }
    }
})
