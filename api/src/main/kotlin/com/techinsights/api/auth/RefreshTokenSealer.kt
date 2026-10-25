package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import org.springframework.stereotype.Component
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM sealing of a refresh token for short-term storage on its predecessor's retired-hash row (idempotent
 * rotation: a retried request re-delivers the same successor). The key is HKDF-derived with its own purpose label.
 * The associated data binds a sealed value to the row it was stored on. Output: base64url(iv || ciphertext || tag).
 */
@Component
class RefreshTokenSealer(
    authProperties: AuthProperties
) {
    private val key = SecretKeySpec(
        KeyPurpose.REFRESH_SUCCESSOR_ENCRYPTION.deriveKey(authProperties.jwt.secretKeyBytes),
        "AES"
    )
    private val random = SecureRandom()

    fun seal(plaintext: String, associatedData: String): String {
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(associatedData.toByteArray())
        val ciphertext = cipher.doFinal(plaintext.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(iv + ciphertext)
    }

    /** @throws IllegalArgumentException if the value is malformed, tampered with, or sealed for different associated data. */
    fun open(sealed: String, associatedData: String): String {
        val bytes = try {
            Base64.getUrlDecoder().decode(sealed)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Sealed value is not valid base64url")
        }
        require(bytes.size > IV_BYTES + TAG_BITS / 8) { "Sealed value is too short" }
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, bytes.copyOfRange(0, IV_BYTES)))
            cipher.updateAAD(associatedData.toByteArray())
            String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES))
        } catch (e: GeneralSecurityException) {
            throw IllegalArgumentException("Sealed value cannot be opened")
        }
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
    }
}
