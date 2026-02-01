package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.ProviderType
import java.util.Optional

interface UserRepository {
    fun findByEmail(email: String): Optional<User>
    fun findByProviderAndProviderId(provider: ProviderType, providerId: String): Optional<User>
    fun save(user: User): User
    fun findById(id: Long): Optional<User>
    fun findByNickname(nickname: String): Optional<User>
    fun existsByNickname(nickname: String): Boolean
}
