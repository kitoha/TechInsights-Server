package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.ProviderType
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository
) : UserRepository {
    override fun findByProviderAndProviderId(provider: ProviderType, providerId: String): Optional<User> {
        return userJpaRepository.findByProviderAndProviderId(provider, providerId)
    }

    override fun findByEmail(email: String): Optional<User> {
        return userJpaRepository.findByEmail(email)
    }

    override fun save(user: User): User {
        return userJpaRepository.save(user)
    }

    override fun findById(id: Long): Optional<User> {
        return userJpaRepository.findById(id)
    }

    override fun findByNickname(nickname: String): Optional<User> {
        return userJpaRepository.findByNickname(nickname)
    }

    override fun existsByNickname(nickname: String): Boolean {
        return userJpaRepository.existsByNickname(nickname)
    }
}
