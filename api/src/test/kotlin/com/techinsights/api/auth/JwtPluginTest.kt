package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import com.techinsights.domain.enums.UserRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Duration

class JwtPluginTest : FunSpec({
    val validSecret = "a".repeat(32) // 32 characters
    val authProperties = AuthProperties(
        jwt = AuthProperties.Jwt(
            secretKey = validSecret,
            accessTokenExpiration = Duration.ofMinutes(1),
            refreshTokenExpiration = Duration.ofDays(1),
            issuer = "test-issuer"
        )
    )
    val jwtPlugin = JwtPlugin(authProperties)

    beforeTest {
        jwtPlugin.init()
    }

    test("비밀키가 32자 미만이면 초기화 시 예외가 발생해야 한다 (Fail-fast)") {
        shouldThrow<IllegalArgumentException> {
            AuthProperties(
                jwt = AuthProperties.Jwt(secretKey = "short")
            )
        }.message shouldBe "Secret key must be at least 32 characters"
    }

    test("액세스 토큰 생성이 정상적으로 이루어져야 한다") {
        val token = jwtPlugin.generateAccessToken(1L, "test@example.com", UserRole.USER)
        token shouldNotBe null
        
        val claims = jwtPlugin.validateToken(token)
        claims.subject shouldBe "1"
        claims["userId"] shouldBe 1
        claims["email"] shouldBe "test@example.com"
        claims["role"] shouldBe UserRole.USER.name
    }

    test("리프레시 토큰 생성이 정상적으로 이루어져야 한다") {
        val token = jwtPlugin.generateRefreshToken(1L)
        token shouldNotBe null
        
        val claims = jwtPlugin.validateToken(token)
        claims.subject shouldBe "1"
        claims["email"] shouldBe null
    }

    test("만료된 토큰 검증 시 ExpiredTokenException이 발생해야 한다") {
        val expiredProps = AuthProperties(
            jwt = AuthProperties.Jwt(
                secretKey = validSecret,
                accessTokenExpiration = Duration.ofMillis(-1000)
            )
        )
        val plugin = JwtPlugin(expiredProps)
        plugin.init()

        val token = plugin.generateAccessToken(1L, "test@example.com", UserRole.USER)
        
        shouldThrow<ExpiredTokenException> {
            plugin.validateToken(token)
        }
    }

    test("위변조된 토큰 검증 시 TokenTamperedException이 발생해야 한다") {
        val token = jwtPlugin.generateAccessToken(1L, "test@example.com", UserRole.USER)
        val tamperedToken = token + "modified"
        
        shouldThrow<TokenTamperedException> {
            jwtPlugin.validateToken(tamperedToken)
        }
    }
})
