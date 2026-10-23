package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import org.springframework.stereotype.Component
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class TokenHasher(
    private val authProperties: AuthProperties
) {
    private val secretKeySpec = SecretKeySpec(
        KeyPurpose.REFRESH_TOKEN_HASHING.deriveKey(authProperties.jwt.secretKeyBytes),
        "HmacSHA256"
    )

    fun hash(token: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        val digest = mac.doFinal(token.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
