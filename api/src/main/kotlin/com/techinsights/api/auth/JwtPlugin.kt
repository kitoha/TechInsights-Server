package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import com.techinsights.domain.enums.UserRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtPlugin(
    private val authProperties: AuthProperties
) {
    private lateinit var key: SecretKey

    @PostConstruct
    fun init() {
        this.key = Keys.hmacShaKeyFor(KeyPurpose.JWT_SIGNING.deriveKey(authProperties.jwt.secretKeyBytes))
    }

    fun generateAccessToken(userId: Long, email: String, role: UserRole): String {
        return generateToken(
            userId = userId,
            email = email,
            role = role,
            type = ACCESS_TYPE,
            expirationMillis = authProperties.jwt.accessTokenExpiration.toMillis()
        )
    }

    fun generateRefreshToken(userId: Long): String {
        return generateToken(
            userId = userId,
            email = null,
            role = null,
            type = REFRESH_TYPE,
            expirationMillis = authProperties.jwt.refreshTokenExpiration.toMillis()
        )
    }

    private fun generateToken(
        userId: Long,
        email: String?,
        role: UserRole?,
        type: String,
        expirationMillis: Long
    ): String {
        val now = Date()
        val expiration = Date(now.time + expirationMillis)

        val claims = Jwts.claims()
            .add("userId", userId)
            .add(TYPE_CLAIM, type)
            .apply {
                email?.let { add("email", it) }
                role?.let { add("role", it.name) }
            }
            .build()

        return Jwts.builder()
            .issuer(authProperties.jwt.issuer)
            .issuedAt(now)
            .expiration(expiration)
            .subject(userId.toString())
            .claims(claims)
            .signWith(key)
            .compact()
    }

    fun validateAccessToken(token: String): Claims = validateToken(token, ACCESS_TYPE)

    fun validateRefreshToken(token: String): Claims = validateToken(token, REFRESH_TYPE)

    private fun validateToken(token: String, expectedType: String): Claims {
        val claims = validateToken(token)
        if (claims.get(TYPE_CLAIM, String::class.java) != expectedType) {
            throw InvalidTokenException("토큰 유형이 올바르지 않습니다.")
        }
        return claims
    }

    fun validateToken(token: String): Claims {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: ExpiredJwtException) {
            throw ExpiredTokenException()
        } catch (e: SignatureException) {
            throw TokenTamperedException()
        } catch (e: MalformedJwtException) {
            throw InvalidTokenException("형식에 맞지 않는 토큰입니다.")
        } catch (e: Exception) {
            throw InvalidTokenException()
        }
    }

    companion object {
        private const val TYPE_CLAIM = "typ"
        private const val ACCESS_TYPE = "access"
        private const val REFRESH_TYPE = "refresh"
    }
}
