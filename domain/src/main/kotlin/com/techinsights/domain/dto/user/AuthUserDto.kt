package com.techinsights.domain.dto.user

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.UserRole

data class AuthUserDto(
    val id: Long,
    val email: String,
    val role: UserRole
) {
    companion object {
        fun fromEntity(user: User): AuthUserDto {
            return AuthUserDto(
                id = user.id,
                email = user.email,
                role = user.role
            )
        }
    }
}
