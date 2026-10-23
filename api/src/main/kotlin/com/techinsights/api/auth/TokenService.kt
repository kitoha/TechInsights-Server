package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import com.techinsights.domain.dto.user.RefreshTokenRotationResult
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.enums.UserStatus
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

    @Transactional(
        noRollbackFor = [TokenTamperedException::class, ExpiredTokenException::class, InvalidTokenException::class]
    )
    fun refresh(oldRefreshToken: String): TokenResponse {
        val claims = jwtPlugin.validateRefreshToken(oldRefreshToken)
        val userId = claims.get("userId", Long::class.javaObjectType)
            ?: throw InvalidTokenException("유효하지 않은 토큰입니다.")

        val user = userService.findAuthUserById(userId)
        if (user == null || user.status != UserStatus.ACTIVE) {
            refreshTokenService.deleteAllByUserId(userId)
            throw InvalidTokenException("유효하지 않은 토큰입니다.")
        }

        val accessToken = jwtPlugin.generateAccessToken(user.id, user.email, user.role)
        val newRefreshToken = jwtPlugin.generateRefreshToken(user.id)
        val expiryAt = Instant.now().plus(authProperties.jwt.refreshTokenExpiration)

        val rotation = refreshTokenService.rotate(
            presentedHash = tokenHasher.hash(oldRefreshToken),
            newHash = tokenHasher.hash(newRefreshToken),
            newExpiryAt = expiryAt
        )

        return when (rotation) {
            RefreshTokenRotationResult.Rotated -> TokenResponse(accessToken, newRefreshToken)
            RefreshTokenRotationResult.Expired -> throw ExpiredTokenException()
            RefreshTokenRotationResult.NotFound -> throw InvalidTokenException("유효하지 않은 토큰입니다.")
            RefreshTokenRotationResult.ReuseDetected -> {
                refreshTokenService.deleteAllByUserId(userId)
                throw TokenTamperedException("재사용된 토큰이 감지되었습니다. 모든 세션이 로그아웃됩니다.")
            }
        }
    }

    @Transactional
    fun revoke(refreshToken: String) {
        val refreshTokenHash = tokenHasher.hash(refreshToken)
        refreshTokenService.deleteByHash(refreshTokenHash)
    }
}
