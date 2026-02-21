package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val tokenService: TokenService,
    private val authProperties: AuthProperties
) {

    @PostMapping("/refresh")
    fun refresh(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<Unit> {
        val refreshToken = request.cookies?.find { it.name == authProperties.jwt.refreshTokenCookieName }?.value
            ?: throw com.techinsights.api.auth.InvalidTokenException("리프레시 토큰이 없습니다.")

        val deviceId = request.getHeader("X-Device-Id") ?: request.getHeader("User-Agent")
        val tokenResponse = tokenService.refresh(refreshToken, deviceId)

        addCookie(response, authProperties.jwt.accessTokenCookieName, tokenResponse.accessToken, authProperties.jwt.accessTokenExpiration.toSeconds())
        addCookie(response, authProperties.jwt.refreshTokenCookieName, tokenResponse.refreshToken, authProperties.jwt.refreshTokenExpiration.toSeconds())

        return ResponseEntity.ok().build()
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<Unit> {
        val refreshToken = request.cookies?.find { it.name == authProperties.jwt.refreshTokenCookieName }?.value
        if (refreshToken != null) {
            tokenService.revoke(refreshToken)
        }

        clearCookie(response, authProperties.jwt.accessTokenCookieName)
        clearCookie(response, authProperties.jwt.refreshTokenCookieName)

        return ResponseEntity.ok().build()
    }

    private fun addCookie(response: HttpServletResponse, name: String, value: String, maxAge: Long) {
        val cookie = ResponseCookie.from(name, value)
            .path("/")
            .httpOnly(true)
            .secure(authProperties.jwt.cookieSecure)
            .sameSite(authProperties.jwt.cookieSameSite)
            .maxAge(maxAge)
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    private fun clearCookie(response: HttpServletResponse, name: String) {
        val cookie = ResponseCookie.from(name, "")
            .path("/")
            .httpOnly(true)
            .secure(authProperties.jwt.cookieSecure)
            .sameSite(authProperties.jwt.cookieSameSite)
            .maxAge(0)
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }
}
