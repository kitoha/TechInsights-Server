package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2SuccessHandler(
    private val tokenService: TokenService,
    private val authProperties: AuthProperties,
    private val authCookies: AuthCookies
) : SimpleUrlAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val userDetails = authentication.principal as CustomUserDetails
        val state = request.getParameter("state")
        val deviceId = DeviceAwareOAuth2AuthorizationRequestResolver.extractDeviceId(state)
        
        val tokens = tokenService.issueTokens(
            userId = userDetails.userId,
            email = userDetails.email,
            role = userDetails.role,
            deviceId = deviceId
        )

        authCookies.add(response, authProperties.jwt.accessTokenCookieName, tokens.accessToken, authProperties.jwt.accessTokenExpiration.toSeconds())
        authCookies.add(response, authProperties.jwt.refreshTokenCookieName, tokens.refreshToken, authProperties.jwt.refreshTokenExpiration.toSeconds())

        redirectStrategy.sendRedirect(request, response, authProperties.oauth2.successRedirectUri)
    }
}
