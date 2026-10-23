package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.ProviderType
import java.util.Optional

interface UserRepository {
    fun findByEmail(email: String): Optional<User>
    fun findByProviderAndProviderId(provider: ProviderType, providerId: String): Optional<User>
    fun save(user: User): User
    /** Creates the user, or refreshes email/name/profileImage/lastLoginAt of the existing (provider, providerId) row, atomically. */
    fun upsertByProvider(
        id: Long,
        email: String,
        name: String,
        nickname: String,
        provider: ProviderType,
        providerId: String,
        profileImage: String?
    )
    fun findById(id: Long): Optional<User>
    fun findByNickname(nickname: String): Optional<User>
    /** Whether another active user already holds [nickname], ignoring case. */
    fun existsByNicknameIgnoreCaseAndIdNot(nickname: String, userId: Long): Boolean
    /**
     * Saves and flushes so a nickname clash surfaces here.
     * @throws DuplicateNicknameException only for a `uk_users_nickname_lower` violation
     */
    fun saveNickname(user: User): User
}
