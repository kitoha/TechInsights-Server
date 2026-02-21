package com.techinsights.domain.dto.user

import com.techinsights.domain.entity.user.RefreshToken
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class RefreshTokenDto(
    val userId: Long,
    val tokenHash: String,
    val previousTokenHash: String?,
    val deviceId: String?,
    val expiryAt: Instant,
    val updatedAt: LocalDateTime?
) {
    fun isExpired(now: Instant = Instant.now()): Boolean {
        return now.isAfter(expiryAt)
    }

    fun isRecentlyRotated(leewaySeconds: Long = 30, now: Instant = Instant.now()): Boolean {
        val lastUpdated = updatedAt ?: return false
        val updatedInstant = lastUpdated.atZone(ZoneId.systemDefault()).toInstant()
        return updatedInstant.isAfter(now.minusSeconds(leewaySeconds))
    }

    companion object {
        fun fromEntity(entity: RefreshToken): RefreshTokenDto {
            return RefreshTokenDto(
                userId = entity.userId,
                tokenHash = entity.tokenHash,
                previousTokenHash = entity.previousTokenHash,
                deviceId = entity.deviceId,
                expiryAt = entity.expiryAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}
