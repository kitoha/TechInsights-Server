package com.techinsights.api.auth

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** HKDF (RFC 5869) with HMAC-SHA256. */
object Hkdf {
    private const val ALGORITHM = "HmacSHA256"
    private const val HASH_LENGTH = 32

    fun deriveSha256(
        ikm: ByteArray,
        info: ByteArray,
        salt: ByteArray = ByteArray(0),
        length: Int = HASH_LENGTH
    ): ByteArray {
        require(length in 1..255 * HASH_LENGTH) { "Requested length must be between 1 and ${255 * HASH_LENGTH}" }

        val prk = hmac(salt.takeIf { it.isNotEmpty() } ?: ByteArray(HASH_LENGTH), ikm)

        val okm = ByteArray(length)
        var previous = ByteArray(0)
        var offset = 0
        var counter = 1
        while (offset < length) {
            previous = hmac(prk, previous, info, byteArrayOf(counter.toByte()))
            val copied = minOf(previous.size, length - offset)
            previous.copyInto(okm, offset, 0, copied)
            offset += copied
            counter++
        }
        return okm
    }

    private fun hmac(key: ByteArray, vararg parts: ByteArray): ByteArray {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(SecretKeySpec(key, ALGORITHM))
        parts.forEach { mac.update(it) }
        return mac.doFinal()
    }
}
