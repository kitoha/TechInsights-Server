package com.techinsights.api.response.user

import com.techinsights.domain.enums.UserRole

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val nickname: String,
    val profileImage: String?,
    val role: UserRole
)
