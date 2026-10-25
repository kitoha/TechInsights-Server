package com.techinsights.api.auth

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class HkdfTest : FunSpec({
    fun hex(value: String): ByteArray = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    test("RFC 5869 test case 1 벡터와 일치해야 한다") {
        val okm = Hkdf.deriveSha256(
            ikm = hex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"),
            salt = hex("000102030405060708090a0b0c"),
            info = hex("f0f1f2f3f4f5f6f7f8f9"),
            length = 42
        )

        okm.toHex() shouldBe "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865"
    }

    test("RFC 5869 test case 3 벡터(빈 salt, 빈 info)와 일치해야 한다") {
        val okm = Hkdf.deriveSha256(
            ikm = hex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"),
            salt = ByteArray(0),
            info = ByteArray(0),
            length = 42
        )

        okm.toHex() shouldBe "8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8"
    }

    test("info가 다르면 서로 다른 키가 파생되어야 한다") {
        val secret = ByteArray(32) { it.toByte() }

        val a = Hkdf.deriveSha256(secret, info = "a".toByteArray())
        val b = Hkdf.deriveSha256(secret, info = "b".toByteArray())

        a.size shouldBe 32
        a.contentEquals(b) shouldBe false
        a.contentEquals(secret) shouldBe false
        a shouldNotBe null
    }

    test("요청 길이가 255 * 32 바이트를 넘으면 예외가 발생해야 한다") {
        shouldThrow<IllegalArgumentException> {
            Hkdf.deriveSha256(ByteArray(32), info = ByteArray(0), length = 255 * 32 + 1)
        }
    }
})
