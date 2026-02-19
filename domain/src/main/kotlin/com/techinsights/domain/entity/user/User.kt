package com.techinsights.domain.entity.user

import com.techinsights.domain.entity.BaseEntity
import com.techinsights.domain.enums.ProviderType
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.enums.UserStatus
import jakarta.persistence.*
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_users_provider_provider_id", columnNames = ["provider", "provider_id"])
    ]
)
class User(
    @Id
    val id: Long,

    @Column(name = "email", nullable = false, unique = true)
    var email: String,

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
) : BaseEntity(), Persistable<Long> {
    @Transient
    private var isNewEntity: Boolean = true

    fun login() {
        this.lastLoginAt = LocalDateTime.now()
    }

    override fun getId(): Long = id

    override fun isNew(): Boolean = isNewEntity

    @PostPersist
    @PostLoad
    private fun markNotNew() {
        isNewEntity = false
    }
}
