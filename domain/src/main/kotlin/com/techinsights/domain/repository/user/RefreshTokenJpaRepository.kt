package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.RefreshToken
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface RefreshTokenJpaRepository : JpaRepository<RefreshToken, Long> {
    fun findByTokenHash(tokenHash: String): Optional<RefreshToken>
    fun findByPreviousTokenHash(previousTokenHash: String): Optional<RefreshToken>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshToken r where r.tokenHash = :tokenHash")
    fun findByTokenHashForUpdate(@Param("tokenHash") tokenHash: String): Optional<RefreshToken>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshToken r where r.previousTokenHash = :previousTokenHash")
    fun findByPreviousTokenHashForUpdate(@Param("previousTokenHash") previousTokenHash: String): Optional<RefreshToken>

    fun deleteByUserId(userId: Long)
    fun findByUserIdAndDeviceId(userId: Long, deviceId: String?): Optional<RefreshToken>
}
