package com.techinsights.api.auth

enum class KeyPurpose(private val label: String) {
    JWT_SIGNING("techinsights/jwt-signing"),
    REFRESH_TOKEN_HASHING("techinsights/refresh-token-hashing"),
    OAUTH_COOKIE_SIGNING("techinsights/oauth-cookie-signing");

    fun deriveKey(secret: ByteArray): ByteArray = Hkdf.deriveSha256(ikm = secret, info = label.toByteArray())
}
