package com.techinsights.domain.dto.user

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.UserRole

data class UserProfileDto(
    val id: Long,
    val email: String,
    val name: String,
    val nickname: String,
    val profileImage: String?,
    val role: UserRole
) {
    companion object {
        fun fromEntity(user: User): UserProfileDto {
            return UserProfileDto(
                id = user.id,
                email = user.email,
                name = user.name,
                nickname = user.nickname,
                profileImage = user.profileImage,
                role = user.role
            )
        }
    }
}
