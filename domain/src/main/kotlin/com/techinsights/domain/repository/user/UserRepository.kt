package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.ProviderType
import java.util.Optional

interface UserRepository {
    fun findByEmail(email: String): Optional<User>
    fun findByProviderAndProviderId(provider: ProviderType, providerId: String): Optional<User>
    fun save(user: User): User
    /** True if an account for (provider, providerId) exists that is soft-deleted or not ACTIVE. */
    fun isDeactivatedAccount(provider: ProviderType, providerId: String): Boolean
    /** Creates the user, or refreshes email/name/profileImage/lastLoginAt of the existing ACTIVE (provider, providerId) row, atomically. Never modifies a soft-deleted or non-ACTIVE row. */
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
    fun existsByNickname(nickname: String): Boolean
}
