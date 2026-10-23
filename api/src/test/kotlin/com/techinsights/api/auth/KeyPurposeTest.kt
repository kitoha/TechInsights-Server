package com.techinsights.api.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class KeyPurposeTest : FunSpec({
    val secret = ByteArray(32) { it.toByte() }

    test("용도별로 서로 다른 키가 파생되어야 한다") {
        val keys = KeyPurpose.entries.map { it.deriveKey(secret).toList() }

        keys.toSet().size shouldBe KeyPurpose.entries.size
    }

    test("같은 시크릿과 용도로는 항상 같은 키가 파생되어야 한다") {
        KeyPurpose.JWT_SIGNING.deriveKey(secret).contentEquals(KeyPurpose.JWT_SIGNING.deriveKey(secret)) shouldBe true
    }

    test("파생된 키는 원본 시크릿과 달라야 한다") {
        KeyPurpose.entries.forEach { it.deriveKey(secret).contentEquals(secret) shouldBe false }
    }

    test("파생된 키는 32바이트여야 한다") {
        KeyPurpose.entries.forEach { it.deriveKey(secret).size shouldBe 32 }
    }
})
