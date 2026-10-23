package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.RefreshToken
import com.techinsights.domain.entity.user.RetiredRefreshToken
import java.time.Instant
import java.util.Optional

interface RefreshTokenRepository {
    fun save(refreshToken: RefreshToken): RefreshToken
    fun findByHashForUpdate(hash: String): Optional<RefreshToken>
    fun findByFamilyIdForUpdate(familyId: Long): Optional<RefreshToken>
    fun findRetiredByHash(hash: String): Optional<RetiredRefreshToken>
    fun retireHash(hash: String, familyId: Long, retiredAt: Instant)
    fun findByUserAndDevice(userId: Long, deviceId: String?): Optional<RefreshToken>
    fun findAllByUserIdOrderByLastUsedDesc(userId: Long): List<RefreshToken>
    fun delete(refreshToken: RefreshToken)
    fun deleteAllByUserId(userId: Long)
    fun deleteExpired(now: Instant): Int
    fun deleteRetiredBefore(cutoff: Instant): Int
}
