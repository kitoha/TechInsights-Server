package com.techinsights.domain.service.user

import com.techinsights.domain.dto.user.AuthUserDto
import com.techinsights.domain.dto.user.UserProfileDto
import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.ProviderType
import com.techinsights.domain.exception.user.DuplicateNicknameException
import com.techinsights.domain.exception.user.UserNotFoundException
import com.techinsights.domain.repository.user.UserRepository
import com.techinsights.domain.utils.Tsid
import com.techinsights.domain.validator.NicknameValidator
import mu.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val nicknameValidator: NicknameValidator
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 사용자 ID로 조회
     *
     * @param userId 조회할 사용자 ID
     * @return User 엔티티
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    fun getUserById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { UserNotFoundException(userId) }
    }

    fun getUserProfileById(userId: Long): UserProfileDto {
        return UserProfileDto.fromEntity(getUserById(userId))
    }

    fun getAuthUserById(userId: Long): AuthUserDto {
        val user = getUserById(userId)
        return AuthUserDto.fromEntity(user)
    }

    /**
     * 닉네임 변경
     * @param userId 사용자 ID
     * @param newNickname 새로운 닉네임
     * @return 업데이트된 User
     */
    @Transactional
    fun updateNickname(userId: Long, newNickname: String): User {
        logger.info { "닉네임 변경 시도: userId=$userId, newNickname=$newNickname" }

        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException(userId) }

        val trimmedNickname = newNickname.trim()
        if (user.nickname == trimmedNickname) {
            logger.info { "기존 닉네임과 동일하여 변경하지 않음: userId=$userId" }
            return user
        }

        nicknameValidator.validate(trimmedNickname)

        if (userRepository.existsByNickname(trimmedNickname)) {
            logger.warn { "중복된 닉네임 변경 시도: nickname=$trimmedNickname" }
            throw DuplicateNicknameException(trimmedNickname)
        }

        user.nickname = trimmedNickname
        val updatedUser = userRepository.save(user)

        logger.info { "닉네임 변경 완료: userId=$userId, newNickname=$trimmedNickname" }
        return updatedUser
    }

    @Transactional
    fun updateNicknameProfile(userId: Long, newNickname: String): UserProfileDto {
        return UserProfileDto.fromEntity(updateNickname(userId, newNickname))
    }

    @Transactional
    fun upsertGoogleOAuthUser(
        providerId: String,
        email: String,
        name: String,
        profileImage: String?
    ): AuthUserDto {
        val user = userRepository.findByProviderAndProviderId(ProviderType.GOOGLE, providerId)
            .map { existingUser ->
                applyGoogleProfile(existingUser, email, name, profileImage)
            }
            .orElseGet {
                createGoogleUser(providerId, email, name, profileImage)
            }

        return try {
            AuthUserDto.fromEntity(userRepository.save(user))
        } catch (e: DataIntegrityViolationException) {
            logger.warn { "Google OAuth upsert race detected: providerId=$providerId, recovering by re-query" }
            recoverFromConcurrentUpsert(providerId, email, name, profileImage, e)
        }
    }

    private fun applyGoogleProfile(
        user: User,
        email: String,
        name: String,
        profileImage: String?
    ): User {
        return user.apply {
            this.email = email
            this.name = name
            this.profileImage = profileImage
            this.login()
        }
    }

    private fun createGoogleUser(
        providerId: String,
        email: String,
        name: String,
        profileImage: String?
    ): User {
        val randomNickname = "User_${Tsid.generate()}"
        return User(
            id = Tsid.decode(Tsid.generate()),
            email = email,
            name = name,
            nickname = randomNickname,
            provider = ProviderType.GOOGLE,
            providerId = providerId,
            profileImage = profileImage,
            lastLoginAt = LocalDateTime.now()
        )
    }

    private fun recoverFromConcurrentUpsert(
        providerId: String,
        email: String,
        name: String,
        profileImage: String?,
        cause: DataIntegrityViolationException
    ): AuthUserDto {
        val existing = userRepository.findByProviderAndProviderId(ProviderType.GOOGLE, providerId)
            .map { applyGoogleProfile(it, email, name, profileImage) }
            .orElseThrow { cause }

        return AuthUserDto.fromEntity(userRepository.save(existing))
    }
}
