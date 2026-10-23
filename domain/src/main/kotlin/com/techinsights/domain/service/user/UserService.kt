package com.techinsights.domain.service.user

import com.techinsights.domain.dto.user.AuthUserDto
import com.techinsights.domain.dto.user.UserProfileDto
import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.ProviderType
import com.techinsights.domain.enums.UserStatus
import com.techinsights.domain.exception.user.AccountDeactivatedException
import com.techinsights.domain.exception.user.DuplicateNicknameException
import com.techinsights.domain.exception.user.UserNotFoundException
import com.techinsights.domain.repository.user.UserRepository
import com.techinsights.domain.utils.Tsid
import com.techinsights.domain.validator.NicknameValidator
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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

    /**
     * 존재하지 않으면 예외 대신 null을 반환한다.
     * 호출자의 트랜잭션에 참여할 때 예외가 프록시를 통과하면 해당 트랜잭션이 rollback-only로 표시되기 때문이다.
     */
    fun findAuthUserById(userId: Long): AuthUserDto? {
        return userRepository.findById(userId)
            .map(AuthUserDto::fromEntity)
            .orElse(null)
    }

    /**
     * 닉네임 변경
     * @param userId 사용자 ID
     * @param newNickname 새로운 닉네임
     * @return 업데이트된 User
     */
    @Transactional
    fun updateNickname(userId: Long, newNickname: String): User {
        logger.debug { "닉네임 변경 시도: userId=$userId" }

        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException(userId) }

        val trimmedNickname = newNickname.trim()
        if (user.nickname == trimmedNickname) {
            logger.debug { "기존 닉네임과 동일하여 변경하지 않음: userId=$userId" }
            return user
        }

        nicknameValidator.validate(trimmedNickname)

        if (userRepository.existsByNicknameIgnoreCaseAndIdNot(trimmedNickname, userId)) {
            logger.info { "중복된 닉네임으로 변경 거절: userId=$userId" }
            throw DuplicateNicknameException(trimmedNickname)
        }

        user.nickname = trimmedNickname
        val updatedUser = userRepository.saveNickname(user)

        logger.info { "닉네임 변경 완료: userId=$userId" }
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
        // A deleted / non-ACTIVE account is rejected before anything is written. The upsert itself also
        // refuses to modify such a row, so a race between this check and the upsert cannot revive it.
        if (userRepository.isDeactivatedAccount(ProviderType.GOOGLE, providerId)) {
            throw AccountDeactivatedException()
        }

        // Single atomic statement: concurrent logins of the same (provider, providerId) cannot race.
        userRepository.upsertByProvider(
            id = Tsid.decode(Tsid.generate()),
            email = email,
            name = name,
            nickname = "User_${Tsid.generate()}",
            provider = ProviderType.GOOGLE,
            providerId = providerId,
            profileImage = profileImage
        )

        val user = userRepository.findByProviderAndProviderId(ProviderType.GOOGLE, providerId).orElse(null)
            ?: run {
                // soft-deleted between the check and the upsert: the entity's restriction hides the row
                if (userRepository.isDeactivatedAccount(ProviderType.GOOGLE, providerId)) {
                    throw AccountDeactivatedException()
                }
                throw IllegalStateException("User not found after upsert: providerId=$providerId")
            }
        if (user.isDeleted || user.status != UserStatus.ACTIVE) {
            throw AccountDeactivatedException()
        }

        return AuthUserDto.fromEntity(user)
    }
}
