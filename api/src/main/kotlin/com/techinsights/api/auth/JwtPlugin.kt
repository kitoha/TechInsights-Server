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
        this.key = Keys.hmacShaKeyFor(authProperties.jwt.secretKey.toByteArray())
    }

    fun generateAccessToken(userId: Long, email: String, role: UserRole): String {
        return generateToken(
            userId = userId,
            email = email,
            role = role,
            expirationMillis = authProperties.jwt.accessTokenExpiration.toMillis()
        )
    }

    fun generateRefreshToken(userId: Long): String {
        return generateToken(
            userId = userId,
            email = null,
            role = null,
            expirationMillis = authProperties.jwt.refreshTokenExpiration.toMillis()
        )
    }

    private fun generateToken(
        userId: Long,
        email: String?,
        role: UserRole?,
        expirationMillis: Long
    ): String {
        val now = Date()
        val expiration = Date(now.time + expirationMillis)

        val claims = Jwts.claims()
            .add("userId", userId)
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
}
