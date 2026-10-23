package com.techinsights.domain.service.user

import com.techinsights.domain.dto.user.RefreshTokenDto
import com.techinsights.domain.dto.user.RefreshTokenRotationResult
import com.techinsights.domain.entity.user.RefreshToken
import com.techinsights.domain.repository.user.RefreshTokenRepository
import com.techinsights.domain.utils.Tsid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository
) {
    @Transactional
    fun upsertToken(
        userId: Long,
        deviceId: String?,
        tokenHash: String,
        expiryAt: Instant
    ): RefreshTokenDto {
        val existing = deviceId?.let { refreshTokenRepository.findByUserAndDevice(userId, it).orElse(null) }
        val tokenEntity = existing?.apply { replaceToken(tokenHash, expiryAt) }
            ?: RefreshToken(
                id = Tsid.decode(Tsid.generate()),
                userId = userId,
                tokenHash = tokenHash,
                deviceId = deviceId,
                expiryAt = expiryAt
            )

        return RefreshTokenDto.fromEntity(refreshTokenRepository.save(tokenEntity))
    }

    @Transactional
    fun rotate(presentedHash: String, newHash: String, newExpiryAt: Instant): RefreshTokenRotationResult {
        val current = refreshTokenRepository.findByHashForUpdate(presentedHash).orElse(null)
        if (current != null) {
            if (current.isExpired()) {
                refreshTokenRepository.delete(current)
                return RefreshTokenRotationResult.Expired
            }
            current.updateToken(newHash, newExpiryAt)
            refreshTokenRepository.save(current)
            return RefreshTokenRotationResult.Rotated
        }

        val rotatedAway = refreshTokenRepository.findByPreviousHashForUpdate(presentedHash).orElse(null)
            ?: return RefreshTokenRotationResult.NotFound
        if (!rotatedAway.isRecentlyRotated()) {
            return RefreshTokenRotationResult.ReuseDetected
        }
        if (rotatedAway.isExpired()) {
            refreshTokenRepository.delete(rotatedAway)
            return RefreshTokenRotationResult.Expired
        }
        rotatedAway.updateToken(newHash, newExpiryAt)
        refreshTokenRepository.save(rotatedAway)
        return RefreshTokenRotationResult.Rotated
    }

    @Transactional(readOnly = true)
    fun findByHash(hash: String): RefreshTokenDto? {
        return refreshTokenRepository.findByHash(hash)
            .map(RefreshTokenDto::fromEntity)
            .orElse(null)
    }

    @Transactional(readOnly = true)
    fun findByPreviousHash(hash: String): RefreshTokenDto? {
        return refreshTokenRepository.findByPreviousHash(hash)
            .map(RefreshTokenDto::fromEntity)
            .orElse(null)
    }

    @Transactional
    fun deleteByHash(hash: String) {
        val session = refreshTokenRepository.findByHashForUpdate(hash).orElse(null)
            ?: refreshTokenRepository.findByPreviousHashForUpdate(hash).orElse(null)
                ?.takeIf { it.isRecentlyRotated() }
        session?.let { refreshTokenRepository.delete(it) }
    }

    @Transactional
    fun deleteAllByUserId(userId: Long) {
        refreshTokenRepository.deleteAllByUserId(userId)
    }
}
