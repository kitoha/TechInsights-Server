package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.ProviderType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface UserJpaRepository : JpaRepository<User, Long> {
    fun findByProviderAndProviderId(provider: ProviderType, providerId: String): Optional<User>
    fun findByEmail(email: String): Optional<User>
    fun findByNickname(nickname: String): Optional<User>

    /** Case-insensitive and limited to active users, mirroring the `uk_users_nickname_lower` partial index. */
    @Query("select count(u) > 0 from User u where lower(u.nickname) = lower(:nickname) and u.id <> :excludeUserId and u.deletedAt is null")
    fun existsByNicknameIgnoreCaseAndIdNot(
        @Param("nickname") nickname: String,
        @Param("excludeUserId") excludeUserId: Long
    ): Boolean

    @Modifying
    @Query(
        value = """
            INSERT INTO users (id, email, name, nickname, provider, provider_id, profile_image,
                               last_login_at, created_at, updated_at)
            VALUES (:id, :email, :name, :nickname, :provider, :providerId, :profileImage,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (provider, provider_id) DO UPDATE
            SET email = EXCLUDED.email,
                name = EXCLUDED.name,
                profile_image = EXCLUDED.profile_image,
                last_login_at = EXCLUDED.last_login_at,
                updated_at = EXCLUDED.updated_at
        """,
        nativeQuery = true
    )
    fun upsertByProvider(
        @Param("id") id: Long,
        @Param("email") email: String,
        @Param("name") name: String,
        @Param("nickname") nickname: String,
        @Param("provider") provider: String,
        @Param("providerId") providerId: String,
        @Param("profileImage") profileImage: String?
    ): Int
}
