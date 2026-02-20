package com.techinsights.domain.service.user

import com.techinsights.domain.dto.user.AuthUserDto
import com.techinsights.domain.enums.ProviderType
import com.techinsights.domain.repository.user.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class GoogleOAuthUserRecoveryService(
    private val userRepository: UserRepository
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recoverFromConcurrentUpsertInNewTx(
        providerId: String,
        email: String,
        name: String,
        profileImage: String?,
        cause: DataIntegrityViolationException
    ): AuthUserDto {
        val existing = userRepository.findByProviderAndProviderId(ProviderType.GOOGLE, providerId)
            .orElseThrow { cause }

        existing.email = email
        existing.name = name
        existing.profileImage = profileImage
        existing.login()

        return AuthUserDto.fromEntity(userRepository.save(existing))
    }
}
