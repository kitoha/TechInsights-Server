package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component

/** The one place that sets / clears the access and refresh auth cookies, so both always carry the same attributes. */
@Component
class AuthCookies(
    private val authProperties: AuthProperties
) {
    fun add(response: HttpServletResponse, name: String, value: String, maxAgeSeconds: Long) {
        response.addHeader(HttpHeaders.SET_COOKIE, builder(name, value).maxAge(maxAgeSeconds).build().toString())
    }

    fun clear(response: HttpServletResponse, name: String) {
        add(response, name, "", 0)
    }

    fun clearAuthCookies(response: HttpServletResponse) {
        clear(response, authProperties.jwt.accessTokenCookieName)
        clear(response, authProperties.jwt.refreshTokenCookieName)
    }

    private fun builder(name: String, value: String): ResponseCookie.ResponseCookieBuilder {
        val builder = ResponseCookie.from(name, value)
            .path("/")
            .httpOnly(true)
            .secure(authProperties.jwt.cookieSecure)
            .sameSite(authProperties.jwt.cookieSameSite)
        authProperties.jwt.cookieDomain
            ?.takeIf { it.isNotBlank() }
            ?.let { builder.domain(it) }
        return builder
    }
}
