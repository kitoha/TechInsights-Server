package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserJpaRepository : JpaRepository<User, Long> {
    fun findByGoogleSub(googleSub: String): Optional<User>
    fun findByEmail(email: String): Optional<User>
}
