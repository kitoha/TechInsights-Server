package com.techinsights.domain.service.user

import com.techinsights.domain.dto.user.RefreshTokenDto
import com.techinsights.domain.dto.user.RefreshTokenRotationResult
import com.techinsights.domain.entity.user.RefreshToken
import com.techinsights.domain.repository.user.RefreshTokenRepository
import com.techinsights.domain.utils.Tsid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository
) {
    /**
     * 로그인은 회전이 아니라 새 세션(패밀리) 시작이다.
     * 같은 (userId, deviceId)의 기존 세션은 폐기하고, 기기 ID가 없으면 항상 새 세션을 만든다.
     * 사용자별 세션이 [maxSessions]를 넘으면 가장 오래 사용되지 않은 세션부터 삭제한다.
     */
    @Transactional
    fun startSession(
        userId: Long,
        deviceId: String?,
        tokenHash: String,
        expiryAt: Instant,
        maxSessions: Int
    ): RefreshTokenDto {
        refreshTokenRepository.lockUserSessions(userId)

        deviceId?.let { refreshTokenRepository.findByUserAndDevice(userId, it).orElse(null) }
            ?.let { refreshTokenRepository.delete(it) }

        val session = refreshTokenRepository.save(
            RefreshToken(
                id = Tsid.generateLong(),
                userId = userId,
                familyId = Tsid.generateLong(),
                tokenHash = tokenHash,
                deviceId = deviceId,
                expiryAt = expiryAt
            )
        )

        refreshTokenRepository.findAllByUserIdOrderByLastUsedDesc(userId)
            .filter { it.id != session.id }
            .drop(maxSessions - 1)
            .sortedBy { it.id }
            .forEach { refreshTokenRepository.delete(it) }

        return RefreshTokenDto.fromEntity(session)
    }

    @Transactional
    fun rotate(presentedHash: String, newHash: String, newExpiryAt: Instant): RefreshTokenRotationResult {
        val now = Instant.now()

        val current = refreshTokenRepository.findByHashForUpdate(presentedHash).orElse(null)
        if (current != null) {
            return rotateSession(current, newHash, newExpiryAt, now)
        }

        val retired = refreshTokenRepository.findRetiredByHash(presentedHash).orElse(null)
            ?: return RefreshTokenRotationResult.NotFound
        val session = refreshTokenRepository.findByFamilyIdForUpdate(retired.familyId).orElse(null)
            ?: return RefreshTokenRotationResult.NotFound

        if (retired.retiredAt.isBefore(now.minus(GRACE_PERIOD))) {
            refreshTokenRepository.delete(session)
            return RefreshTokenRotationResult.ReuseDetected
        }
        return rotateSession(session, newHash, newExpiryAt, now)
    }

    @Transactional
    fun deleteByHash(hash: String) {
        val session = refreshTokenRepository.findByHashForUpdate(hash).orElse(null)
            ?: refreshTokenRepository.findRetiredByHash(hash).orElse(null)
                ?.takeIf { !it.retiredAt.isBefore(Instant.now().minus(GRACE_PERIOD)) }
                ?.let { refreshTokenRepository.findByFamilyIdForUpdate(it.familyId).orElse(null) }
        session?.let { refreshTokenRepository.delete(it) }
    }

    @Transactional
    fun deleteAllByUserId(userId: Long) {
        refreshTokenRepository.deleteAllByUserId(userId)
    }

    @Transactional
    fun deleteExpired(now: Instant = Instant.now()): Int = refreshTokenRepository.deleteExpired(now)

    @Transactional
    fun deleteRetiredBefore(cutoff: Instant): Int = refreshTokenRepository.deleteRetiredBefore(cutoff)

    private fun rotateSession(
        session: RefreshToken,
        newHash: String,
        newExpiryAt: Instant,
        now: Instant
    ): RefreshTokenRotationResult {
        if (session.isExpired()) {
            refreshTokenRepository.delete(session)
            return RefreshTokenRotationResult.Expired
        }
        refreshTokenRepository.retireHash(session.tokenHash, session.familyId, now)
        session.rotateTo(newHash, newExpiryAt)
        refreshTokenRepository.save(session)
        return RefreshTokenRotationResult.Rotated
    }

    companion object {
        private val GRACE_PERIOD = Duration.ofSeconds(30)
    }
}
