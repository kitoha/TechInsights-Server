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
        val cookies = arrayOf(Cookie("__Host-ti-rt", oldRt))

        every { request.cookies } returns cookies
        every { request.getHeader("X-Device-Id") } returns "device-uuid-123"
        every { tokenService.refresh(oldRt, "device-uuid-123") } returns TokenResponse("new-at", "new-rt")
        every { response.addHeader(any(), any()) } just Runs

        val result = controller.refresh(request, response)

        result.statusCode.value() shouldBe 200
        verify { response.addHeader("Set-Cookie", match { it.contains("__Host-ti-at=new-at") && it.contains("Secure") }) }
        verify { response.addHeader("Set-Cookie", match { it.contains("__Host-ti-rt=new-rt") && it.contains("Secure") }) }
    }

    test("X-Device-Id 헤더가 없으면 null deviceId로 refresh해야 한다 (User-Agent fallback 없음)") {
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>()
        val cookies = arrayOf(Cookie("__Host-ti-rt", "old-rt"))

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
        val cookies = arrayOf(Cookie("__Host-ti-rt", rt))

        every { request.cookies } returns cookies
        every { tokenService.revoke(rt) } just Runs
        every { response.addHeader(any(), any()) } just Runs

        val result = controller.logout(request, response)

        result.statusCode.value() shouldBe 200
        verify { tokenService.revoke(rt) }
        verify { response.addHeader("Set-Cookie", match { it.contains("__Host-ti-at=") && it.contains("Max-Age=0") && it.contains("Secure") }) }
        verify { response.addHeader("Set-Cookie", match { it.contains("__Host-ti-rt=") && it.contains("Max-Age=0") && it.contains("Secure") }) }
    }
})
