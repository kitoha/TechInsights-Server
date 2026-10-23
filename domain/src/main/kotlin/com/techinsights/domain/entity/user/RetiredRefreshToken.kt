package com.techinsights.domain.entity.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "refresh_token_retired_hashes")
class RetiredRefreshToken(
    @Id
    @Column(name = "token_hash", nullable = false, columnDefinition = "TEXT")
    val tokenHash: String,

    @Column(name = "family_id", nullable = false)
    val familyId: Long,

    @Column(name = "retired_at", nullable = false)
    val retiredAt: Instant
)
