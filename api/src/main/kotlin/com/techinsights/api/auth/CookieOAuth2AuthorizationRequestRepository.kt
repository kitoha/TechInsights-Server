package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import com.fasterxml.jackson.databind.cfg.CoercionAction
import com.fasterxml.jackson.databind.cfg.CoercionInputShape
import com.fasterxml.jackson.databind.type.LogicalType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component
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
    private val log = KotlinLogging.logger {}
    // No scalar coercion: a number / boolean where a string belongs is an unparseable cookie, not a silently converted one.
    private val objectMapper = jacksonObjectMapper().apply {
        coercionConfigFor(LogicalType.Textual).apply {
            setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
            setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
            setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
        }
    }
    private val signingKey = SecretKeySpec(
        KeyPurpose.OAUTH_COOKIE_SIGNING.deriveKey(authProperties.jwt.secretKeyBytes),
        SIGNATURE_ALGORITHM
    )

    override fun loadAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? {
        val cookieValue = request.cookies
            ?.firstOrNull { it.name == oauth2Properties.authorizationRequestCookieName }
            ?.value
            ?: return missingCookie(request)

        val payload = verifyAndExtractPayload(cookieValue) ?: return invalidCookie(request)

        return runCatching {
            objectMapper.readValue<OAuth2AuthorizationRequestCookiePayload>(Base64.getUrlDecoder().decode(payload))
                .toAuthorizationRequest()
        }.onFailure {
            log.warn {
                "OAuth2 authorization request cookie could not be parsed: " +
                    "uri=${request.requestURI}, exception=${it::class.simpleName}"
            }
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
            .encodeToString(
                objectMapper.writeValueAsBytes(OAuth2AuthorizationRequestCookiePayload.from(authorizationRequest))
            )
        val signedValue = "$encoded.${sign(encoded)}"

        val secure = isSecureRequest(request)
        val domain = oauth2Properties.authorizationRequestCookieDomain?.takeIf { it.isNotBlank() }

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            cookieBuilder(request, signedValue)
                .maxAge(oauth2Properties.authorizationRequestCookieMaxAge)
                .build()
                .toString()
        )

        log.info {
            "OAuth2 authorization request cookie issued: " +
                "name=${oauth2Properties.authorizationRequestCookieName}, " +
                "domain=${domain ?: "<host-only>"}, " +
                "sameSite=${oauth2Properties.authorizationRequestCookieSameSite}, " +
                "secure=$secure, " +
                "host=${request.serverName}, " +
                "forwardedHost=${request.getHeader("X-Forwarded-Host") ?: "-"}, " +
                "forwardedProto=${request.getHeader("X-Forwarded-Proto") ?: "-"}"
        }
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
        val builder = ResponseCookie.from(oauth2Properties.authorizationRequestCookieName, value)
            .httpOnly(true)
            .secure(isSecureRequest(request))
            .sameSite(oauth2Properties.authorizationRequestCookieSameSite)
            .path("/")
        oauth2Properties.authorizationRequestCookieDomain
            ?.takeIf { it.isNotBlank() }
            ?.let { builder.domain(it) }
        return builder
    }

    private fun isSecureRequest(request: HttpServletRequest): Boolean {
        return request.isSecure || request.getHeader("X-Forwarded-Proto")?.equals("https", true) == true
    }

    private fun missingCookie(request: HttpServletRequest): OAuth2AuthorizationRequest? {
        if (isOAuth2Callback(request)) {
            val cookieNames = request.cookies?.joinToString(",") { it.name }.orEmpty()
            log.warn {
                "OAuth2 authorization request cookie is missing: " +
                    "uri=${request.requestURI}, expected=${oauth2Properties.authorizationRequestCookieName}, " +
                    "cookies=[$cookieNames], " +
                    "host=${request.serverName}, " +
                    "forwardedHost=${request.getHeader("X-Forwarded-Host") ?: "-"}, " +
                    "configuredDomain=${oauth2Properties.authorizationRequestCookieDomain ?: "<host-only>"}"
            }
        }
        return null
    }

    private fun invalidCookie(request: HttpServletRequest): OAuth2AuthorizationRequest? {
        log.warn {
            "OAuth2 authorization request cookie signature is invalid: " +
                "uri=${request.requestURI}, " +
                "host=${request.serverName}, " +
                "forwardedHost=${request.getHeader("X-Forwarded-Host") ?: "-"}"
        }
        return null
    }

    private fun isOAuth2Callback(request: HttpServletRequest): Boolean {
        return request.requestURI.startsWith("/login/oauth2/code/")
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
        mac.init(signingKey)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.toByteArray()))
    }

    companion object {
        private const val SIGNATURE_ALGORITHM = "HmacSHA256"
    }
}
