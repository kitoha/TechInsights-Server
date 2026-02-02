package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RefreshTokenJpaRepository : JpaRepository<RefreshToken, Long> {
    fun findByTokenHash(tokenHash: String): Optional<RefreshToken>
    fun findByPreviousTokenHash(previousTokenHash: String): Optional<RefreshToken>
    fun deleteByUserId(userId: Long)
    fun findByUserIdAndDeviceId(userId: Long, deviceId: String?): Optional<RefreshToken>
}
