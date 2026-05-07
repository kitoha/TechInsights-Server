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

        redirectStrategy.sendRedirect(
            request,
            response,
            "${authProperties.oauth2.successRedirectUri}?error=login_failed"
        )
    }
}
