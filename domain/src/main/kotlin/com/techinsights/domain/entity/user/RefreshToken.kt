package com.techinsights.domain.entity.user

import com.techinsights.domain.entity.BaseEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "refresh_tokens", indexes = [
    Index(name = "idx_rt_hash", columnList = "token_hash", unique = true),
    Index(name = "idx_rt_user_device", columnList = "user_id, device_id")
])
class RefreshToken(
    @Id
    val id: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "token_hash", nullable = false, length = 255)
    var tokenHash: String,

    @Column(name = "device_id", nullable = true, length = 100)
    val deviceId: String? = null,

    @Column(name = "expiry_at", nullable = false)
    var expiryAt: Instant
) : BaseEntity() {

    fun updateToken(newHash: String, newExpiry: Instant) {
        this.tokenHash = newHash
        this.expiryAt = newExpiry
    }

    fun isExpired(): Boolean = Instant.now().isAfter(expiryAt)
}
