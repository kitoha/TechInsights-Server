package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.service.user.RefreshTokenService
import com.techinsights.domain.service.user.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class TokenService(
    private val jwtPlugin: JwtPlugin,
    private val refreshTokenService: RefreshTokenService,
    private val userService: UserService,
    private val authProperties: AuthProperties,
    private val tokenHasher: TokenHasher
) {
    @Transactional
    fun issueTokens(userId: Long, email: String, role: UserRole, deviceId: String?): TokenResponse {
        val accessToken = jwtPlugin.generateAccessToken(userId, email, role)
        val refreshTokenString = jwtPlugin.generateRefreshToken(userId)
        val refreshTokenHash = tokenHasher.hash(refreshTokenString)
        
        val expiryAt = Instant.now().plus(authProperties.jwt.refreshTokenExpiration)

        refreshTokenService.upsertToken(
            userId = userId,
            deviceId = deviceId,
            tokenHash = refreshTokenHash,
            expiryAt = expiryAt
        )
        
        return TokenResponse(accessToken, refreshTokenString)
    }

    @Transactional
    fun refresh(oldRefreshToken: String, deviceId: String?): TokenResponse {
        val claims = jwtPlugin.validateToken(oldRefreshToken)
        val userId = claims.get("userId", Long::class.javaObjectType)
        val refreshTokenHash = tokenHasher.hash(oldRefreshToken)

        val user = runCatching { userService.getAuthUserById(userId) }
            .getOrNull()
            ?: throw com.techinsights.api.auth.InvalidTokenException("사용자를 찾을 수 없습니다.")

        val storedToken = refreshTokenService.findByHash(refreshTokenHash)

        if (storedToken == null) {
            val rotatedToken = refreshTokenService.findByPreviousHash(refreshTokenHash)
            if (rotatedToken != null && rotatedToken.isRecentlyRotated()) {
                return issueTokens(user.id, user.email, user.role, deviceId)
            }

            refreshTokenService.deleteAllByUserId(userId)
            throw com.techinsights.api.auth.TokenTamperedException("재사용된 토큰이 감지되었습니다. 모든 세션이 로그아웃됩니다.")
        }

        if (storedToken.isExpired()) {
            refreshTokenService.deleteByHash(refreshTokenHash)
            throw com.techinsights.api.auth.ExpiredTokenException()
        }
        
        return issueTokens(user.id, user.email, user.role, deviceId)
    }

    @Transactional
    fun revoke(refreshToken: String) {
        val refreshTokenHash = tokenHasher.hash(refreshToken)
        refreshTokenService.deleteByHash(refreshTokenHash)
    }
}
