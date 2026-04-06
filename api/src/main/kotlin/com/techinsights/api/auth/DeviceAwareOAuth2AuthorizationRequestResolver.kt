package com.techinsights.api.auth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest

class DeviceAwareOAuth2AuthorizationRequestResolver(
    clientRegistrationRepository: ClientRegistrationRepository
) : OAuth2AuthorizationRequestResolver {

    private val delegate = DefaultOAuth2AuthorizationRequestResolver(
        clientRegistrationRepository,
        "/oauth2/authorization"
    )

    override fun resolve(request: HttpServletRequest): OAuth2AuthorizationRequest? {
        val authRequest = delegate.resolve(request) ?: return null
        return embedDeviceId(authRequest, request)
    }

    override fun resolve(
        request: HttpServletRequest,
        clientRegistrationId: String
    ): OAuth2AuthorizationRequest? {
        val authRequest = delegate.resolve(request, clientRegistrationId) ?: return null
        return embedDeviceId(authRequest, request)
    }

    private fun embedDeviceId(
        authRequest: OAuth2AuthorizationRequest,
        request: HttpServletRequest
    ): OAuth2AuthorizationRequest {
        val newState = appendDeviceId(authRequest.state, request)
        return OAuth2AuthorizationRequest.from(authRequest)
            .state(newState)
            .build()
    }

    fun appendDeviceId(state: String, request: HttpServletRequest): String {
        val deviceId = request.getParameter("deviceId")
            ?.takeIf { it.isNotBlank() }
            ?.takeIf { it.length <= 128 }
            ?.takeIf { DEVICE_ID_PATTERN.matches(it) }
            ?: return state
        return "$state|$deviceId"
    }

    companion object {
        private val DEVICE_ID_PATTERN = Regex("[a-zA-Z0-9\\-_]+")

        fun extractDeviceId(state: String?): String? {
            if (state == null || "|" !in state) return null
            return state.substringAfter("|").ifBlank { null }
        }
    }
}
