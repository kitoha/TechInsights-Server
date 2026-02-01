package com.techinsights.domain.entity.user

import com.techinsights.domain.entity.BaseEntity
import com.techinsights.domain.enums.ProviderType
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.enums.UserStatus
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    val id: Long,

    @Column(name = "email", nullable = false, unique = true)
    val email: String,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "nickname", unique = true)
    var nickname: String,

    @Column(name = "provider", nullable = false)
    @Enumerated(EnumType.STRING)
    val provider: ProviderType,

    @Column(name = "provider_id", nullable = false)
    val providerId: String,

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    var role: UserRole = UserRole.USER,

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: UserStatus = UserStatus.ACTIVE,

    @Column(name = "profile_image")
    var profileImage: String? = null,

    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null,

    @Column(name = "marketing_agreed", nullable = false)
    var marketingAgreed: Boolean = false
) : BaseEntity() {

    fun login() {
        this.lastLoginAt = LocalDateTime.now()
    }
}
