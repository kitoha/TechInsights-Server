package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component
import org.springframework.util.SerializationUtils
import java.security.MessageDigest
import java.time.Duration
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class CookieOAuth2AuthorizationRequestRepository(
    private val authProperties: AuthProperties
) : AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    private val oauth2Properties = authProperties.oauth2

    override fun loadAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? {
        val cookieValue = request.cookies
            ?.firstOrNull { it.name == oauth2Properties.authorizationRequestCookieName }
            ?.value
            ?: return null

        val payload = verifyAndExtractPayload(cookieValue) ?: return null

        return runCatching {
            @Suppress("DEPRECATION")
            SerializationUtils.deserialize(Base64.getUrlDecoder().decode(payload)) as? OAuth2AuthorizationRequest
        }.getOrNull()
    }

    override fun saveAuthorizationRequest(
        authorizationRequest: OAuth2AuthorizationRequest?,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        if (authorizationRequest == null) {
            clearCookie(request, response)
            return
        }

        val encoded = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(SerializationUtils.serialize(authorizationRequest))
        val signedValue = "$encoded.${sign(encoded)}"

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            cookieBuilder(request, signedValue)
                .maxAge(oauth2Properties.authorizationRequestCookieMaxAge)
                .build()
                .toString()
        )
    }

    override fun removeAuthorizationRequest(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): OAuth2AuthorizationRequest? {
        val authorizationRequest = loadAuthorizationRequest(request)
        clearCookie(request, response)
        return authorizationRequest
    }

    private fun clearCookie(request: HttpServletRequest, response: HttpServletResponse) {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            cookieBuilder(request, "")
                .maxAge(Duration.ZERO)
                .build()
                .toString()
        )
    }

    private fun cookieBuilder(request: HttpServletRequest, value: String): ResponseCookie.ResponseCookieBuilder {
        return ResponseCookie.from(oauth2Properties.authorizationRequestCookieName, value)
            .httpOnly(true)
            .secure(isSecureRequest(request))
            .sameSite(oauth2Properties.authorizationRequestCookieSameSite)
            .path("/")
    }

    private fun isSecureRequest(request: HttpServletRequest): Boolean {
        return request.isSecure || request.getHeader("X-Forwarded-Proto")?.equals("https", true) == true
    }

    private fun verifyAndExtractPayload(value: String): String? {
        val payload = value.substringBefore(".", missingDelimiterValue = "")
        val signature = value.substringAfter(".", missingDelimiterValue = "")
        if (payload.isBlank() || signature.isBlank()) return null
        return payload.takeIf {
            MessageDigest.isEqual(sign(it).toByteArray(), signature.toByteArray())
        }
    }

    private fun sign(value: String): String {
        val mac = Mac.getInstance(SIGNATURE_ALGORITHM)
        mac.init(SecretKeySpec(authProperties.jwt.secretKeyBytes, SIGNATURE_ALGORITHM))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.toByteArray()))
    }

    companion object {
        private const val SIGNATURE_ALGORITHM = "HmacSHA256"
    }
}
