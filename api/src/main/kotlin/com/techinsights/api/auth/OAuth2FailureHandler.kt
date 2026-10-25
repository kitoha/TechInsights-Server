package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class OAuth2FailureHandler(
    private val authProperties: AuthProperties,
    private val authCookies: AuthCookies
) : SimpleUrlAuthenticationFailureHandler() {
    private val log = KotlinLogging.logger {}

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        // Only the error code and exception class: provider descriptions / messages can carry user data.
        val errorCode = (exception as? OAuth2AuthenticationException)?.error?.errorCode ?: "-"
        log.warn {
            "OAuth2 login failed: exception=${exception::class.simpleName}, errorCode=$errorCode, uri=${request.requestURI}"
        }

        authCookies.clearAuthCookies(response)

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
}
