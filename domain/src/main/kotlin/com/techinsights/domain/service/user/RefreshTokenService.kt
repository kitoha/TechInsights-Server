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
    /**
     * Idempotent rotation. Presenting the current token rotates it and stores [sealedSuccessor] (the new refresh token,
     * sealed by the api) on the retired row of the presented hash. Presenting that just-retired hash again inside the
     * grace window — a retried or parallel refresh whose response was lost — re-delivers the same successor instead of
     * rotating again. An older retired hash, or any retired hash after the window, revokes the family.
     */
    fun rotate(
        presentedHash: String,
        newHash: String,
        newExpiryAt: Instant,
        sealedSuccessor: String
    ): RefreshTokenRotationResult {
        val now = Instant.now()

        val current = refreshTokenRepository.findByHashForUpdate(presentedHash).orElse(null)
        if (current != null) {
            return rotateSession(current, newHash, newExpiryAt, sealedSuccessor, now)
        }

        val retired = refreshTokenRepository.findRetiredByHash(presentedHash).orElse(null)
            ?: return RefreshTokenRotationResult.NotFound
        val session = refreshTokenRepository.findByFamilyIdForUpdate(retired.familyId).orElse(null)
            ?: return RefreshTokenRotationResult.NotFound

        // Only the hash retired by the family's latest rotation, and only inside the grace window, is a benign retry /
        // parallel refresh (the browser does not single-flight refreshes and responses can be lost): it gets the same
        // successor again. Anything else (outside grace, or an older retired hash) is reuse and revokes the family.
        val justRetired = refreshTokenRepository.findLatestRetiredHash(retired.familyId).orElse(null) == presentedHash
        val withinGrace = !retired.retiredAt.isBefore(now.minus(GRACE_PERIOD))
        if (!justRetired || !withinGrace) {
            refreshTokenRepository.delete(session)
            return RefreshTokenRotationResult.ReuseDetected
        }
        if (session.isExpired()) {
            refreshTokenRepository.delete(session)
            return RefreshTokenRotationResult.Expired
        }
        // A legacy retired row has no stored successor: it cannot be re-delivered, so fail closed.
        val sealedSuccessorOfRetired = refreshTokenRepository.findSealedSuccessor(presentedHash).orElse(null)
        if (sealedSuccessorOfRetired == null) {
            refreshTokenRepository.delete(session)
            return RefreshTokenRotationResult.ReuseDetected
        }
        return RefreshTokenRotationResult.GraceRedelivery(sealedSuccessorOfRetired)
    }

    /**
     * Logout. Presenting the current token ends the session; presenting a retired token of a session
     * (inside or outside grace) is treated like reuse and revokes that session (family).
     */
    @Transactional
    fun deleteByHash(hash: String) {
        val session = refreshTokenRepository.findByHashForUpdate(hash).orElse(null)
            ?: refreshTokenRepository.findRetiredByHash(hash).orElse(null)
                ?.let { refreshTokenRepository.findByFamilyIdForUpdate(it.familyId).orElse(null) }
        session?.let { refreshTokenRepository.delete(it) }
    }

    @Transactional
    fun deleteAllByUserId(userId: Long) {
        refreshTokenRepository.deleteAllByUserId(userId)
    }

    @Transactional
    fun deleteExpired(now: Instant = Instant.now()): Int = refreshTokenRepository.deleteExpired(now)

    /** Clears sealed successors whose grace window has passed (they are unusable then and must not linger). */
    @Transactional
    fun clearExpiredSuccessors(now: Instant = Instant.now()): Int =
        refreshTokenRepository.clearSealedSuccessorsBefore(now.minus(GRACE_PERIOD))

    @Transactional
    fun deleteRetiredBefore(cutoff: Instant): Int = refreshTokenRepository.deleteRetiredBefore(cutoff)

    private fun rotateSession(
        session: RefreshToken,
        newHash: String,
        newExpiryAt: Instant,
        sealedSuccessor: String,
        now: Instant
    ): RefreshTokenRotationResult {
        if (session.isExpired()) {
            refreshTokenRepository.delete(session)
            return RefreshTokenRotationResult.Expired
        }
        // Older retired rows of this family are past their grace window: drop their sealed successors on this touch.
        refreshTokenRepository.clearSealedSuccessorsInFamilyBefore(session.familyId, now.minus(GRACE_PERIOD))
        refreshTokenRepository.retireHash(session.tokenHash, session.familyId, now, sealedSuccessor)
        session.rotateTo(newHash, newExpiryAt)
        refreshTokenRepository.save(session)
        return RefreshTokenRotationResult.Rotated
    }

    companion object {
        private val GRACE_PERIOD = Duration.ofSeconds(30)
    }
}
