package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.User
import java.util.Optional

interface UserRepository {
    fun findByGoogleSub(googleSub: String): Optional<User>
    fun findByEmail(email: String): Optional<User>
    fun save(user: User): User
    fun findById(id: Long): Optional<User>
}
