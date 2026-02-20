package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2SuccessHandler(
    private val tokenService: TokenService,
    private val authProperties: AuthProperties
) : SimpleUrlAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val userDetails = authentication.principal as CustomUserDetails
        val deviceId = request.getHeader("X-Device-Id") ?: request.getHeader("User-Agent")
        
        val tokens = tokenService.issueTokens(
            userId = userDetails.userId,
            email = userDetails.email,
            role = userDetails.role,
            deviceId = deviceId
        )

        addCookie(response, authProperties.jwt.accessTokenCookieName, tokens.accessToken, authProperties.jwt.accessTokenExpiration.toSeconds())
        addCookie(response, authProperties.jwt.refreshTokenCookieName, tokens.refreshToken, authProperties.jwt.refreshTokenExpiration.toSeconds())

        redirectStrategy.sendRedirect(request, response, authProperties.oauth2.successRedirectUri)
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
}
