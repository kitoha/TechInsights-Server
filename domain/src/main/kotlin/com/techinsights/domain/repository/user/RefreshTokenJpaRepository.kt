package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.RefreshToken
import com.techinsights.domain.entity.user.RetiredRefreshToken
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.Optional

interface RefreshTokenJpaRepository : JpaRepository<RefreshToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshToken r where r.tokenHash = :tokenHash")
    fun findByTokenHashForUpdate(@Param("tokenHash") tokenHash: String): Optional<RefreshToken>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshToken r where r.familyId = :familyId")
    fun findByFamilyIdForUpdate(@Param("familyId") familyId: Long): Optional<RefreshToken>

    @Query("select r from RetiredRefreshToken r where r.tokenHash = :tokenHash")
    fun findRetiredByTokenHash(@Param("tokenHash") tokenHash: String): Optional<RetiredRefreshToken>

    @Modifying
    @Query(
        value = """
            INSERT INTO refresh_token_retired_hashes (token_hash, family_id, retired_at)
            VALUES (:tokenHash, :familyId, :retiredAt)
            ON CONFLICT (token_hash) DO NOTHING
        """,
        nativeQuery = true
    )
    fun insertRetired(
        @Param("tokenHash") tokenHash: String,
        @Param("familyId") familyId: Long,
        @Param("retiredAt") retiredAt: Instant
    ): Int

    /** Atomically claims the single grace re-rotation of a retired hash; 0 rows means it was already used. */
    @Modifying
    @Query(
        value = "UPDATE refresh_token_retired_hashes SET grace_used = TRUE WHERE token_hash = :tokenHash AND grace_used = FALSE",
        nativeQuery = true
    )
    fun claimGraceUse(@Param("tokenHash") tokenHash: String): Int

    @Modifying
    @Query(
        value = "DELETE FROM refresh_token_retired_hashes WHERE retired_at < :cutoff",
        nativeQuery = true
    )
    fun deleteRetiredBefore(@Param("cutoff") cutoff: Instant): Int

    @Modifying
    @Query(value = "DELETE FROM refresh_tokens WHERE expiry_at < :now", nativeQuery = true)
    fun deleteExpiredBefore(@Param("now") now: Instant): Int

    /**
     * Two-int advisory lock: namespace 1414681427 ('TISS', fixed for user-session serialization) + a 32-bit hash of the
     * user id, so it never shares the single-bigint key space with any other advisory lock. User ids are 64-bit Tsids and
     * do not fit an int; a hash collision only makes two users' logins briefly serialize (never a correctness issue).
     */
    @Query(
        value = "SELECT 1 FROM (SELECT pg_advisory_xact_lock(1414681427, hashtext(CAST(:userId AS text)))) AS locked",
        nativeQuery = true
    )
    fun lockUserSessions(@Param("userId") userId: Long): Int

    fun findByUserIdOrderByUpdatedAtDesc(userId: Long): List<RefreshToken>
    fun deleteByUserId(userId: Long)
    fun findByUserIdAndDeviceId(userId: Long, deviceId: String?): Optional<RefreshToken>
}
