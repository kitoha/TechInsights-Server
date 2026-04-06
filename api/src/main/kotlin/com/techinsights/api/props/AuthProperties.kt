package com.techinsights.api.props

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration
import java.util.Base64

@ConfigurationProperties("auth")
data class AuthProperties(
    val jwt: Jwt = Jwt(),
    val oauth2: OAuth2 = OAuth2()
) {
    data class Jwt(
        val secretKey: String = "",
        val accessTokenExpiration: Duration = Duration.ofMinutes(30),
        val refreshTokenExpiration: Duration = Duration.ofDays(30),
        val issuer: String = "techinsights",
        val accessTokenCookieName: String = "__Host-ti-at",
        val refreshTokenCookieName: String = "__Host-ti-rt",
        val cookieSecure: Boolean = true,
        val cookieSameSite: String = "Lax"
    ) {
        init {
            require(secretKey.isNotBlank()) { "Secret key must not be empty" }
            require(secretKey.length >= 32) { "Secret key must be at least 32 characters" }
            require(isValidBase64(secretKey)) { "Secret key must be valid Base64" }
            require(Base64.getDecoder().decode(secretKey).size >= 32) {
                "Secret key decoded bytes must be at least 32 bytes (256 bits) for HMAC-SHA256"
            }
        }

        val secretKeyBytes: ByteArray = Base64.getDecoder().decode(secretKey)

        private fun isValidBase64(value: String): Boolean =
            runCatching { Base64.getDecoder().decode(value); true }.getOrDefault(false)
    }

    data class OAuth2(
        val successRedirectUri: String = "/"
    )
}
