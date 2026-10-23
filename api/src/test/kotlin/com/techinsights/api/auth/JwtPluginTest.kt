package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import com.techinsights.domain.enums.UserRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.time.Duration
import java.util.Date
import java.util.Base64

class JwtPluginTest : FunSpec({
    val validSecret = Base64.getEncoder().encodeToString("a".repeat(32).toByteArray())
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

    test("액세스 토큰에는 typ=access 클레임이 담겨야 한다") {
        val token = jwtPlugin.generateAccessToken(1L, "test@example.com", UserRole.USER)

        jwtPlugin.validateToken(token)["typ"] shouldBe "access"
    }

    test("리프레시 토큰에는 typ=refresh 클레임이 담겨야 한다") {
        val token = jwtPlugin.generateRefreshToken(1L)

        jwtPlugin.validateToken(token)["typ"] shouldBe "refresh"
    }

    test("validateAccessToken은 액세스 토큰만 허용해야 한다") {
        val access = jwtPlugin.generateAccessToken(1L, "test@example.com", UserRole.USER)
        val refresh = jwtPlugin.generateRefreshToken(1L)

        jwtPlugin.validateAccessToken(access).subject shouldBe "1"
        shouldThrow<WrongTokenTypeException> { jwtPlugin.validateAccessToken(refresh) }
    }

    test("validateRefreshToken은 리프레시 토큰만 허용해야 한다") {
        val access = jwtPlugin.generateAccessToken(1L, "test@example.com", UserRole.USER)
        val refresh = jwtPlugin.generateRefreshToken(1L)

        jwtPlugin.validateRefreshToken(refresh).subject shouldBe "1"
        shouldThrow<WrongTokenTypeException> { jwtPlugin.validateRefreshToken(access) }
    }

    test("typ 클레임이 없는 토큰은 두 검증 모두에서 거부되어야 한다") {
        val untyped = Jwts.builder()
            .subject("1")
            .claim("userId", 1L)
            .expiration(Date(System.currentTimeMillis() + 60_000))
            .signWith(Keys.hmacShaKeyFor(KeyPurpose.JWT_SIGNING.deriveKey(authProperties.jwt.secretKeyBytes)))
            .compact()

        shouldThrow<InvalidTokenException> { jwtPlugin.validateAccessToken(untyped) }
        shouldThrow<InvalidTokenException> { jwtPlugin.validateRefreshToken(untyped) }
    }

    test("원본 시크릿으로 직접 서명한 토큰은 거부되어야 한다") {
        val signedWithRawSecret = Jwts.builder()
            .subject("1")
            .claim("typ", "access")
            .expiration(Date(System.currentTimeMillis() + 60_000))
            .signWith(Keys.hmacShaKeyFor(authProperties.jwt.secretKeyBytes))
            .compact()

        shouldThrow<TokenTamperedException> { jwtPlugin.validateAccessToken(signedWithRawSecret) }
    }

    test("같은 사용자의 리프레시 토큰은 같은 초에 발급되어도 서로 달라야 한다") {
        val tokens = (1..20).map { jwtPlugin.generateRefreshToken(1L) }

        tokens.toSet().size shouldBe tokens.size
    }
})
