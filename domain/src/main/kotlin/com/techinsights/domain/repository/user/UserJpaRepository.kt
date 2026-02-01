package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.ProviderType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserJpaRepository : JpaRepository<User, Long> {
    fun findByProviderAndProviderId(provider: ProviderType, providerId: String): Optional<User>
    fun findByEmail(email: String): Optional<User>
    fun findByNickname(nickname: String): Optional<User>
    fun existsByNickname(nickname: String): Boolean
}
