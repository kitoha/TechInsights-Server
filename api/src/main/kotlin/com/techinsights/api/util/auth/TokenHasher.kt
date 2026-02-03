package com.techinsights.api.util.auth

import com.techinsights.api.props.AuthProperties
import org.springframework.stereotype.Component
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class TokenHasher(
    private val authProperties: AuthProperties
) {
    fun hash(token: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(authProperties.jwt.secretKey.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        val digest = mac.doFinal(token.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
