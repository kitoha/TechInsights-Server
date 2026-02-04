package com.techinsights.domain.service.user

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.exception.user.DuplicateNicknameException
import com.techinsights.domain.exception.user.UserNotFoundException
import com.techinsights.domain.repository.user.UserRepository
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
}
