package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.RefreshToken
import com.techinsights.domain.entity.user.RetiredRefreshToken
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.Optional

@Repository
class RefreshTokenRepositoryImpl(
    private val jpaRepository: RefreshTokenJpaRepository
) : RefreshTokenRepository {

    override fun save(refreshToken: RefreshToken): RefreshToken {
        return jpaRepository.save(refreshToken)
    }

    override fun findByHashForUpdate(hash: String): Optional<RefreshToken> =
        jpaRepository.findByTokenHashForUpdate(hash)

    override fun findByFamilyIdForUpdate(familyId: Long): Optional<RefreshToken> =
        jpaRepository.findByFamilyIdForUpdate(familyId)

    override fun findRetiredByHash(hash: String): Optional<RetiredRefreshToken> =
        jpaRepository.findRetiredByTokenHash(hash)

    override fun retireHash(hash: String, familyId: Long, retiredAt: Instant) {
        jpaRepository.insertRetired(hash, familyId, retiredAt)
    }

    override fun lockUserSessions(userId: Long) {
        jpaRepository.lockUserSessions(userId)
    }

    override fun findByUserAndDevice(userId: Long, deviceId: String?): Optional<RefreshToken> {
        return jpaRepository.findByUserIdAndDeviceId(userId, deviceId)
    }

    override fun findAllByUserIdOrderByLastUsedDesc(userId: Long): List<RefreshToken> =
        jpaRepository.findByUserIdOrderByUpdatedAtDesc(userId)

    override fun delete(refreshToken: RefreshToken) {
        jpaRepository.delete(refreshToken)
    }

    @Transactional
    override fun deleteAllByUserId(userId: Long) {
        jpaRepository.deleteByUserId(userId)
    }

    @Transactional
    override fun deleteExpired(now: Instant): Int = jpaRepository.deleteExpiredBefore(now)

    @Transactional
    override fun deleteRetiredBefore(cutoff: Instant): Int = jpaRepository.deleteRetiredBefore(cutoff)
}
