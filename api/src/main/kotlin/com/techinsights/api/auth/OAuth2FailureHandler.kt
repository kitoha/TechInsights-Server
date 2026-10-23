package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class OAuth2FailureHandler(
    private val authProperties: AuthProperties
) : SimpleUrlAuthenticationFailureHandler() {
    private val log = KotlinLogging.logger {}

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        if (exception is OAuth2AuthenticationException) {
            log.warn {
                "OAuth2 login failed: errorCode=${exception.error.errorCode}, " +
                    "description=${exception.error.description}, uri=${request.requestURI}, " +
                    "message=${exception.message}"
            }
        } else {
            log.warn {
                "OAuth2 login failed: exception=${exception::class.simpleName}, " +
                    "uri=${request.requestURI}, message=${exception.message}"
            }
        }

        clearCookie(response, authProperties.jwt.accessTokenCookieName)
        clearCookie(response, authProperties.jwt.refreshTokenCookieName)

        redirectStrategy.sendRedirect(
            request,
            response,
            "${authProperties.oauth2.successRedirectUri}?error=${errorParameter(exception)}"
        )
    }

    private fun errorParameter(exception: AuthenticationException): String {
        val deactivated = exception is OAuth2AuthenticationException &&
            exception.error.errorCode == CustomOAuth2UserService.ACCOUNT_DEACTIVATED_ERROR_CODE
        return if (deactivated) "account_deactivated" else "login_failed"
    }

    private fun clearCookie(response: HttpServletResponse, name: String) {
        val builder = ResponseCookie.from(name, "")
            .path("/")
            .httpOnly(true)
            .secure(authProperties.jwt.cookieSecure)
            .sameSite(authProperties.jwt.cookieSameSite)
            .maxAge(0)
        authProperties.jwt.cookieDomain
            ?.takeIf { it.isNotBlank() }
            ?.let { builder.domain(it) }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString())
    }
}
