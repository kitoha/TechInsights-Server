package com.techinsights.domain.dto.user

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.enums.UserStatus

data class AuthUserDto(
    val id: Long,
    val email: String,
    val role: UserRole,
    val status: UserStatus,
    /** Read from deleted_at on the loaded row, so it holds whether or not the entity's soft-delete restriction applies. */
    val isDeleted: Boolean
) {
    companion object {
        fun fromEntity(user: User): AuthUserDto {
            return AuthUserDto(
                id = user.id,
                email = user.email,
                role = user.role,
                status = user.status,
                isDeleted = user.isDeleted
            )
        }
    }
}
