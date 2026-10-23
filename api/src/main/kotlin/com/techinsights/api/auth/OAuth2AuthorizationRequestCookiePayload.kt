package com.techinsights.api.auth

import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest

data class OAuth2AuthorizationRequestCookiePayload(
    val authorizationUri: String,
    val clientId: String,
    val redirectUri: String?,
    val scopes: Set<String>,
    val state: String?,
    val additionalParameters: Map<String, String>,
    val attributes: Map<String, String>,
    val authorizationRequestUri: String?
) {
    fun toAuthorizationRequest(): OAuth2AuthorizationRequest {
        val builder = OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri(authorizationUri)
            .clientId(clientId)
            .redirectUri(redirectUri)
            .scopes(scopes)
            .state(state)
            .additionalParameters(additionalParameters)
            .attributes(attributes)
        authorizationRequestUri?.let { builder.authorizationRequestUri(it) }
        return builder.build()
    }

    companion object {
        fun from(request: OAuth2AuthorizationRequest): OAuth2AuthorizationRequestCookiePayload {
            require(request.grantType == AuthorizationGrantType.AUTHORIZATION_CODE) {
                "Only authorization_code requests are supported"
            }
            return OAuth2AuthorizationRequestCookiePayload(
                authorizationUri = request.authorizationUri,
                clientId = request.clientId,
                redirectUri = request.redirectUri,
                scopes = request.scopes,
                state = request.state,
                additionalParameters = request.additionalParameters.mapValues { it.value.toString() },
                attributes = request.attributes.mapValues { it.value.toString() },
                authorizationRequestUri = request.authorizationRequestUri
            )
        }
    }
}
