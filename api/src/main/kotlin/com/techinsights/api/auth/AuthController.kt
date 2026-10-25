package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val tokenService: TokenService,
    private val authProperties: AuthProperties,
    private val authCookies: AuthCookies
) {

    @PostMapping("/refresh")
    fun refresh(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<Unit> {
        val refreshToken = request.cookies?.find { it.name == authProperties.jwt.refreshTokenCookieName }?.value
            ?: throw InvalidTokenException("리프레시 토큰이 없습니다.")

        val tokenResponse = tokenService.refresh(refreshToken)

        authCookies.add(response, authProperties.jwt.accessTokenCookieName, tokenResponse.accessToken, authProperties.jwt.accessTokenExpiration.toSeconds())
        authCookies.add(response, authProperties.jwt.refreshTokenCookieName, tokenResponse.refreshToken, authProperties.jwt.refreshTokenExpiration.toSeconds())

        return ResponseEntity.ok().build()
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<Unit> {
        val refreshToken = request.cookies?.find { it.name == authProperties.jwt.refreshTokenCookieName }?.value
        if (refreshToken != null) {
            tokenService.revoke(refreshToken)
        }

        authCookies.clearAuthCookies(response)

        return ResponseEntity.ok().build()
    }
}
