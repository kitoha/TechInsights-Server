package com.techinsights.domain.entity.user

import com.techinsights.domain.entity.BaseEntity
import com.techinsights.domain.enums.UserRole
import jakarta.persistence.*

@Entity
@Table(name = "users")
class User(
    @Id
    val id: Long,

    @Column(name = "email", nullable = false, unique = true)
    val email: String,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "google_sub", nullable = false, unique = true)
    val googleSub: String,

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    var role: UserRole = UserRole.USER,

    @Column(name = "profile_image")
    var profileImage: String? = null
) : BaseEntity()
