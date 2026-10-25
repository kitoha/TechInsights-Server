package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.Base64

class RefreshTokenSealerTest : FunSpec({
    fun propsWith(secret: String) = AuthProperties(
        jwt = AuthProperties.Jwt(secretKey = Base64.getEncoder().encodeToString(secret.toByteArray()))
    )

    val sealer = RefreshTokenSealer(propsWith("this-is-a-very-secure-secret-key!!"))
    val token = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.signature-part"

    test("봉인한 토큰을 같은 aad 로 열면 원문이 나온다") {
        sealer.open(sealer.seal(token, "hash-1"), "hash-1") shouldBe token
    }

    test("봉인된 값에는 원문이 평문으로 들어 있지 않다") {
        val sealed = sealer.seal(token, "hash-1")

        sealed.contains(token) shouldBe false
        sealed.contains("eyJ") shouldBe false
        String(Base64.getUrlDecoder().decode(sealed), Charsets.ISO_8859_1).contains("signature-part") shouldBe false
    }

    test("같은 입력도 매번 다른 값으로 봉인된다 (랜덤 IV)") {
        val first = sealer.seal(token, "hash-1")
        val second = sealer.seal(token, "hash-1")

        (first == second) shouldBe false
    }

    test("다른 aad(다른 폐기 해시 행)로는 열리지 않는다") {
        val sealed = sealer.seal(token, "hash-1")

        shouldThrow<IllegalArgumentException> { sealer.open(sealed, "hash-2") }
    }

    test("변조된 값은 열리지 않는다") {
        val bytes = Base64.getUrlDecoder().decode(sealer.seal(token, "hash-1"))
        bytes[bytes.size - 1] = (bytes.last().toInt() xor 1).toByte()
        val tampered = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        shouldThrow<IllegalArgumentException> { sealer.open(tampered, "hash-1") }
    }

    test("다른 시크릿으로 파생한 키로는 열리지 않는다") {
        val other = RefreshTokenSealer(propsWith("another-very-secure-secret-key-32b!!"))

        shouldThrow<IllegalArgumentException> { other.open(sealer.seal(token, "hash-1"), "hash-1") }
    }

    test("형식이 잘못된 입력은 IllegalArgumentException") {
        shouldThrow<IllegalArgumentException> { sealer.open("not base64 !!", "hash-1") }
        shouldThrow<IllegalArgumentException> { sealer.open("", "hash-1") }
        shouldThrow<IllegalArgumentException> { sealer.open("AAAA", "hash-1") }
    }
})
