package com.techinsights.api.response.user

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.UserRole

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val nickname: String,
    val profileImage: String?,
    val role: UserRole
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
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
