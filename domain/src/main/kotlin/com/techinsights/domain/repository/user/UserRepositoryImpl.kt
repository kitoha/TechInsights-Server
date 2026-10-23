package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.ProviderType
import com.techinsights.domain.exception.user.DuplicateNicknameException
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
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

    override fun isDeactivatedAccount(provider: ProviderType, providerId: String): Boolean {
        return userJpaRepository.countDeactivatedByProvider(provider.name, providerId) > 0
    }

    override fun upsertByProvider(
        id: Long,
        email: String,
        name: String,
        nickname: String,
        provider: ProviderType,
        providerId: String,
        profileImage: String?
    ) {
        userJpaRepository.upsertByProvider(id, email, name, nickname, provider.name, providerId, profileImage)
    }

    override fun findById(id: Long): Optional<User> {
        return userJpaRepository.findById(id)
    }

    override fun findByNickname(nickname: String): Optional<User> {
        return userJpaRepository.findByNickname(nickname)
    }

    override fun existsByNicknameIgnoreCaseAndIdNot(nickname: String, userId: Long): Boolean {
        return userJpaRepository.existsByNicknameIgnoreCaseAndIdNot(nickname, userId)
    }

    override fun saveNickname(user: User): User {
        try {
            return userJpaRepository.saveAndFlush(user)
        } catch (e: DataIntegrityViolationException) {
            if (e.violates(NICKNAME_UNIQUE_INDEX)) {
                throw DuplicateNicknameException(user.nickname)
            }
            throw e
        }
    }

    private fun Throwable.violates(constraint: String): Boolean =
        generateSequence(this) { it.cause }
            .filterIsInstance<ConstraintViolationException>()
            .any { it.constraintName?.endsWith(constraint) == true || it.message?.contains(constraint) == true }

    companion object {
        private const val NICKNAME_UNIQUE_INDEX = "uk_users_nickname_lower"
    }
}
