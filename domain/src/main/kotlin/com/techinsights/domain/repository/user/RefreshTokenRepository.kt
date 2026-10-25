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
    /** 사용자별 세션 생성/정리를 직렬화한다 (트랜잭션 종료 시 해제). */
    fun lockUserSessions(userId: Long)
    fun findByUserAndDevice(userId: Long, deviceId: String?): Optional<RefreshToken>
    fun findAllByUserIdOrderByLastUsedDesc(userId: Long): List<RefreshToken>
    fun delete(refreshToken: RefreshToken)
    fun deleteAllByUserId(userId: Long)
    fun deleteExpired(now: Instant): Int
    fun deleteRetiredBefore(cutoff: Instant): Int
}
