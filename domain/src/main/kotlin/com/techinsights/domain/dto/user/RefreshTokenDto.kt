package com.techinsights.domain.dto.user

import com.techinsights.domain.entity.user.RefreshToken
import java.time.Instant

data class RefreshTokenDto(
    val userId: Long,
    val familyId: Long,
    val tokenHash: String,
    val deviceId: String?,
    val expiryAt: Instant
) {
    fun isExpired(now: Instant = Instant.now()): Boolean {
        return now.isAfter(expiryAt)
    }

    companion object {
        fun fromEntity(entity: RefreshToken): RefreshTokenDto {
            return RefreshTokenDto(
                userId = entity.userId,
                familyId = entity.familyId,
                tokenHash = entity.tokenHash,
                deviceId = entity.deviceId,
                expiryAt = entity.expiryAt
            )
        }
    }
}
