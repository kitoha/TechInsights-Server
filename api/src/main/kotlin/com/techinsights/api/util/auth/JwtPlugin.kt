package com.techinsights.api.util.auth

import com.techinsights.api.exception.auth.ExpiredTokenException
import com.techinsights.api.exception.auth.InvalidTokenException
import com.techinsights.api.exception.auth.TokenTamperedException
import com.techinsights.domain.enums.UserRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtPlugin(
    @Value("\${auth.jwt.secret-key}")
    private val secretKey: String,
    @Value("\${auth.jwt.access-token-expiration-days:1}")
    private val accessTokenExpirationDays: Long,
    @Value("\${auth.jwt.refresh-token-expiration-days:30}")
    private val refreshTokenExpirationDays: Long,
    @Value("\${auth.jwt.issuer:techinsights}")
    private val issuer: String
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secretKey.toByteArray(StandardCharsets.UTF_8))

    fun generateAccessToken(userId: Long, email: String, role: UserRole): String {
        return generateToken(userId, email, role, accessTokenExpirationDays)
    }

    fun generateRefreshToken(userId: Long): String {
        return generateToken(userId, null, null, refreshTokenExpirationDays)
    }

    private fun generateToken(userId: Long, email: String?, role: UserRole?, expirationDays: Long): String {
        val now = Instant.now()
        val expiration = now.plus(expirationDays, ChronoUnit.DAYS)

        val builder = Jwts.builder()
            .issuer(issuer)
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .claim("userId", userId)
        
        email?.let { builder.claim("email", it) }
        role?.let { builder.claim("role", it.name) }

        return builder.signWith(key).compact()
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
