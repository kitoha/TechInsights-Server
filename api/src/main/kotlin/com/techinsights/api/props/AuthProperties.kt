package com.techinsights.api.props

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import java.time.Duration

@ConfigurationProperties("auth")
data class AuthProperties @ConstructorBinding constructor(
    val jwt: Jwt = Jwt(),
    val oauth2: OAuth2 = OAuth2()
) {
    data class Jwt(
        val secretKey: String = "",
        val accessTokenExpiration: Duration = Duration.ofMinutes(30),
        val refreshTokenExpiration: Duration = Duration.ofDays(30),
        val issuer: String = "techinsights",
        val accessTokenCookieName: String = "at",
        val refreshTokenCookieName: String = "rt",
        val cookieSecure: Boolean = true,
        val cookieSameSite: String = "Lax"
    ) {
        init {
            require(secretKey.isNotBlank()) { "Secret key must not be empty" }
            require(secretKey.length >= 32) { "Secret key must be at least 32 characters" }
        }
    }

    data class OAuth2(
        val successRedirectUri: String = "/"
    )
}
