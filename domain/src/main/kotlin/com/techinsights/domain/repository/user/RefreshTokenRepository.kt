package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.RefreshToken
import java.util.Optional

interface RefreshTokenRepository {
    fun save(refreshToken: RefreshToken): RefreshToken
    fun findByHash(hash: String): Optional<RefreshToken>
    fun findByUserAndDevice(userId: Long, deviceId: String?): Optional<RefreshToken>
    fun delete(refreshToken: RefreshToken)
    fun deleteAllByUserId(userId: Long)
}
