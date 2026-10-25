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
    val authorizationRequestUri: String?,
    /**
     * Epoch seconds the cookie was issued; inside the signed payload so it cannot be extended by the client.
     * Optional on read (cookies issued before this field existed); a missing value is treated as expired.
     */
    val issuedAt: Long? = null
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
        fun from(request: OAuth2AuthorizationRequest, issuedAt: Long): OAuth2AuthorizationRequestCookiePayload {
            require(request.grantType == AuthorizationGrantType.AUTHORIZATION_CODE) {
                "Only authorization_code requests are supported"
            }
            return OAuth2AuthorizationRequestCookiePayload(
                authorizationUri = request.authorizationUri,
                clientId = request.clientId,
                redirectUri = request.redirectUri,
                scopes = request.scopes,
                state = request.state,
                additionalParameters = request.additionalParameters.requireStringValues("additionalParameters"),
                attributes = request.attributes.requireStringValues("attributes"),
                authorizationRequestUri = request.authorizationRequestUri,
                issuedAt = issuedAt
            )
        }

        private fun Map<String, Any>.requireStringValues(name: String): Map<String, String> =
            mapValues { (key, value) ->
                require(value is String) {
                    "$name[$key] must be a String to be stored in the cookie, was ${value::class.simpleName}"
                }
                value
            }
    }
}
