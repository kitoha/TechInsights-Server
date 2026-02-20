package com.techinsights.domain.service.user

import com.techinsights.domain.dto.user.RefreshTokenDto
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
        val tokenEntity = refreshTokenRepository.findByUserAndDevice(userId, deviceId)
            .map { existing ->
                existing.apply {
                    updateToken(tokenHash, expiryAt)
                }
            }
            .orElseGet {
                RefreshToken(
                    id = Tsid.decode(Tsid.generate()),
                    userId = userId,
                    tokenHash = tokenHash,
                    deviceId = deviceId,
                    expiryAt = expiryAt
                )
            }

        return RefreshTokenDto.fromEntity(refreshTokenRepository.save(tokenEntity))
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
        refreshTokenRepository.findByHash(hash).ifPresent {
            refreshTokenRepository.delete(it)
        }
    }

    @Transactional
    fun deleteAllByUserId(userId: Long) {
        refreshTokenRepository.deleteAllByUserId(userId)
    }
}
