package com.techinsights.domain.entity.user

import com.techinsights.domain.entity.BaseEntity
import jakarta.persistence.*
import java.time.Instant
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "refresh_tokens", indexes = [
    Index(name = "idx_rt_hash", columnList = "token_hash", unique = true),
    Index(name = "idx_rt_user_device", columnList = "user_id, device_id")
])
@SQLRestriction("deleted_at IS NULL")
class RefreshToken(
    @Id
    val id: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "family_id", nullable = false, updatable = false)
    val familyId: Long,

    @Column(name = "token_hash", nullable = false, columnDefinition = "TEXT")
    var tokenHash: String,

    @Column(name = "device_id", nullable = true, columnDefinition = "TEXT")
    val deviceId: String? = null,

    @Column(name = "expiry_at", nullable = false)
    var expiryAt: Instant
) : BaseEntity() {

    fun rotateTo(newHash: String, newExpiry: Instant) {
        this.tokenHash = newHash
        this.expiryAt = newExpiry
    }

    fun isExpired(): Boolean = Instant.now().isAfter(expiryAt)
}
