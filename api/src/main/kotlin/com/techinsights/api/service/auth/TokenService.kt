package com.techinsights.api.service.auth

import com.techinsights.api.props.AuthProperties
import com.techinsights.api.response.auth.TokenResponse
import com.techinsights.api.util.auth.JwtPlugin
import com.techinsights.api.util.auth.TokenHasher
import com.techinsights.domain.entity.user.RefreshToken
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.repository.user.RefreshTokenRepository
import com.techinsights.domain.repository.user.UserRepository
import com.techinsights.domain.utils.Tsid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class TokenService(
    private val jwtPlugin: JwtPlugin,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val authProperties: AuthProperties,
    private val tokenHasher: TokenHasher
) {
    @Transactional
    fun issueTokens(userId: Long, email: String, role: UserRole, deviceId: String?): TokenResponse {
        val accessToken = jwtPlugin.generateAccessToken(userId, email, role)
        val refreshTokenString = jwtPlugin.generateRefreshToken(userId)
        val refreshTokenHash = tokenHasher.hash(refreshTokenString)
        
        val expiryAt = Instant.now().plus(authProperties.jwt.refreshTokenExpiration)
        
        val refreshTokenEntity = refreshTokenRepository.findByUserAndDevice(userId, deviceId)
            .map { existing ->
                existing.apply {
                    updateToken(refreshTokenHash, expiryAt)
                }
            }
            .orElseGet {
                RefreshToken(
                    id = Tsid.decode(Tsid.generate()),
                    userId = userId,
                    tokenHash = refreshTokenHash,
                    deviceId = deviceId,
                    expiryAt = expiryAt
                )
            }
        
        refreshTokenRepository.save(refreshTokenEntity)
        
        return TokenResponse(accessToken, refreshTokenString)
    }

    @Transactional
    fun refresh(oldRefreshToken: String, deviceId: String?): TokenResponse {
        val claims = jwtPlugin.validateToken(oldRefreshToken)
        val userId = claims.get("userId", Long::class.javaObjectType)
        val refreshTokenHash = tokenHasher.hash(oldRefreshToken)
        
        val storedTokenOpt = refreshTokenRepository.findByHash(refreshTokenHash)

        val user = userRepository.findById(userId)
            .orElseThrow { com.techinsights.api.exception.auth.InvalidTokenException("사용자를 찾을 수 없습니다.") }
        
        if (storedTokenOpt.isEmpty) {
            return refreshTokenRepository.findByPreviousHash(refreshTokenHash)
                .filter { it.isRecentlyRotated() }
                .map { issueTokens(user.id, user.email, user.role, deviceId) }
                .orElseThrow {
                    refreshTokenRepository.deleteAllByUserId(userId)
                    com.techinsights.api.exception.auth.TokenTamperedException("재사용된 토큰이 감지되었습니다. 모든 세션이 로그아웃됩니다.")
                }
        }

        val storedToken = storedTokenOpt.get()
        if (storedToken.isExpired()) {
            refreshTokenRepository.delete(storedToken)
            throw com.techinsights.api.exception.auth.ExpiredTokenException()
        }
        
        return issueTokens(user.id, user.email, user.role, deviceId)
    }

    @Transactional
    fun revoke(refreshToken: String) {
        val refreshTokenHash = tokenHasher.hash(refreshToken)
        refreshTokenRepository.findByHash(refreshTokenHash).ifPresent {
            refreshTokenRepository.delete(it)
        }
    }
}
