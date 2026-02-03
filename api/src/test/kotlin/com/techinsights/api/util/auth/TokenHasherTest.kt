package com.techinsights.api.util.auth

import com.techinsights.api.props.AuthProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import java.time.Duration

class TokenHasherTest : FunSpec({
    val authProperties = AuthProperties(
        jwt = AuthProperties.Jwt(
            secretKey = "this-is-a-very-secure-secret-key-for-testing-purposes-only",
            refreshTokenExpiration = Duration.ofDays(30)
        )
    )
    val tokenHasher = TokenHasher(authProperties)

    test("동일한 토큰은 항상 동일한 해시를 생성해야 한다") {
        // given
        val token = "sample-refresh-token-12345"

        // when
        val hash1 = tokenHasher.hash(token)
        val hash2 = tokenHasher.hash(token)

        // then
        hash1 shouldBe hash2
    }

    test("다른 토큰은 다른 해시를 생성해야 한다") {
        // given
        val token1 = "token-1"
        val token2 = "token-2"

        // when
        val hash1 = tokenHasher.hash(token1)
        val hash2 = tokenHasher.hash(token2)

        // then
        hash1 shouldNotBe hash2
    }

    test("해시는 Base64 URL-safe 인코딩되어야 한다 (패딩 없음)") {
        // given
        val token = "sample-token"

        // when
        val hash = tokenHasher.hash(token)

        // then
        // Base64 URL-safe는 +, /, = 문자를 사용하지 않음
        hash.contains("+") shouldBe false
        hash.contains("/") shouldBe false
        hash.contains("=") shouldBe false
    }

    test("해시 길이는 HmacSHA256의 출력 길이와 일치해야 한다") {
        // given
        val token = "any-token"

        // when
        val hash = tokenHasher.hash(token)

        // then
        // HmacSHA256는 32바이트(256비트) 출력
        // Base64 인코딩 시 약 43자 (32 * 4/3 = 42.67, 패딩 없이)
        hash shouldHaveLength 43
    }

    test("빈 문자열도 해시 가능해야 한다") {
        // given
        val emptyToken = ""

        // when
        val hash = tokenHasher.hash(emptyToken)

        // then
        hash shouldNotBe ""
        hash shouldHaveLength 43
    }

    test("매우 긴 토큰도 동일한 길이의 해시를 생성해야 한다") {
        // given
        val shortToken = "short"
        val longToken = "a".repeat(10000)

        // when
        val shortHash = tokenHasher.hash(shortToken)
        val longHash = tokenHasher.hash(longToken)

        // then
        shortHash shouldHaveLength 43
        longHash shouldHaveLength 43
    }

    test("특수 문자가 포함된 토큰도 올바르게 해시되어야 한다") {
        // given
        val specialToken = "token!@#$%^&*()_+-={}[]|\\:;\"'<>,.?/~`"

        // when
        val hash = tokenHasher.hash(specialToken)

        // then
        hash shouldNotBe ""
        hash shouldHaveLength 43
    }

    test("JWT 형식의 토큰을 해시할 수 있어야 한다") {
        // given
        val jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsImVtYWlsIjoidGVzdEBleGFtcGxlLmNvbSJ9.abcdefg123456"

        // when
        val hash = tokenHasher.hash(jwtToken)

        // then
        hash shouldNotBe ""
        hash shouldHaveLength 43
    }

    test("한글이 포함된 토큰도 올바르게 해시되어야 한다") {
        // given
        val koreanToken = "토큰-한글-테스트-12345"

        // when
        val hash = tokenHasher.hash(koreanToken)

        // then
        hash shouldNotBe ""
        hash shouldHaveLength 43
    }

    test("연속된 동일 문자도 고유한 해시를 생성해야 한다") {
        // given
        val token1 = "aaaa"
        val token2 = "bbbb"

        // when
        val hash1 = tokenHasher.hash(token1)
        val hash2 = tokenHasher.hash(token2)

        // then
        hash1 shouldNotBe hash2
    }

    test("토큰의 순서가 바뀌면 다른 해시가 생성되어야 한다") {
        // given
        val token1 = "abc123"
        val token2 = "123abc"

        // when
        val hash1 = tokenHasher.hash(token1)
        val hash2 = tokenHasher.hash(token2)

        // then
        hash1 shouldNotBe hash2
    }

    test("다른 시크릿 키를 사용하면 다른 해시가 생성되어야 한다") {
        // given
        val token = "same-token"
        val otherAuthProperties = AuthProperties(
            jwt = AuthProperties.Jwt(
                secretKey = "different-secret-key-for-testing",
                refreshTokenExpiration = Duration.ofDays(30)
            )
        )
        val otherTokenHasher = TokenHasher(otherAuthProperties)

        // when
        val hash1 = tokenHasher.hash(token)
        val hash2 = otherTokenHasher.hash(token)

        // then
        hash1 shouldNotBe hash2
    }
})
