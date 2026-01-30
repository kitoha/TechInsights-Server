package com.techinsights.api.util.auth

import com.techinsights.api.exception.auth.ExpiredTokenException
import com.techinsights.api.exception.auth.InvalidTokenException
import com.techinsights.api.exception.auth.TokenTamperedException
import com.techinsights.domain.enums.UserRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.*

class JwtPluginTest : FunSpec({
    val secretKey = "test-secret-key-at-least-32-characters-long!!"
    val accessTokenExpirationDays = 1L
    val refreshTokenExpirationDays = 30L
    val issuer = "techinsights"

    val jwtPlugin = JwtPlugin(
        secretKey = secretKey,
        accessTokenExpirationDays = accessTokenExpirationDays,
        refreshTokenExpirationDays = refreshTokenExpirationDays,
        issuer = issuer
    )

    test("Access 성 토큰을 생성할 수 있어야 한다") {
        val userId = 100L
        val email = "test@example.com"
        val role = UserRole.USER

        val token = jwtPlugin.generateAccessToken(userId, email, role)

        token shouldNotBe null
        val claims = jwtPlugin.validateToken(token)
        claims.get("userId", Long::class.javaObjectType) shouldBe userId
        claims.get("email", String::class.java) shouldBe email
        claims.get("role", String::class.java) shouldBe role.name
    }

    test("Refresh 토큰을 생성할 수 있어야 한다") {
        val userId = 200L

        val token = jwtPlugin.generateRefreshToken(userId)

        token shouldNotBe null
        val claims = jwtPlugin.validateToken(token)
        claims.get("userId", Long::class.javaObjectType) shouldBe userId
    }

    test("만료된 토큰은 ExpiredTokenException을 던져야 한다") {
        val shortLivedPlugin = JwtPlugin(
            secretKey = secretKey,
            accessTokenExpirationDays = -1L,
            refreshTokenExpirationDays = -1L,
            issuer = issuer
        )
        val token = shortLivedPlugin.generateAccessToken(1L, "test@example.com", UserRole.USER)
        
        shouldThrow<ExpiredTokenException> {
            jwtPlugin.validateToken(token)
        }
    }

    test("변조된 토큰은 TokenTamperedException을 던져야 한다") {
        val token = jwtPlugin.generateAccessToken(1L, "test@example.com", UserRole.USER)
        val tamperedToken = token + "modified"
        
        shouldThrow<TokenTamperedException> {
            jwtPlugin.validateToken(tamperedToken)
        }
    }

    test("잘못된 형식의 토큰은 InvalidTokenException을 던져야 한다") {
        val invalidToken = "not.a.jwt"
        
        shouldThrow<InvalidTokenException> {
            jwtPlugin.validateToken(invalidToken)
        }
    }
})
