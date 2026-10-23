package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.RefreshToken
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Repository
class RefreshTokenRepositoryImpl(
    private val jpaRepository: RefreshTokenJpaRepository
) : RefreshTokenRepository {
    
    override fun save(refreshToken: RefreshToken): RefreshToken {
        return jpaRepository.save(refreshToken)
    }

    override fun findByHash(hash: String): Optional<RefreshToken> {
        return jpaRepository.findByTokenHash(hash)
    }

    override fun findByPreviousHash(hash: String): Optional<RefreshToken> =
        jpaRepository.findByPreviousTokenHash(hash)

    override fun findByHashForUpdate(hash: String): Optional<RefreshToken> =
        jpaRepository.findByTokenHashForUpdate(hash)

    override fun findByPreviousHashForUpdate(hash: String): Optional<RefreshToken> =
        jpaRepository.findByPreviousTokenHashForUpdate(hash)

    override fun findByUserAndDevice(userId: Long, deviceId: String?): Optional<RefreshToken> {
        return jpaRepository.findByUserIdAndDeviceId(userId, deviceId)
    }

    override fun delete(refreshToken: RefreshToken) {
        jpaRepository.delete(refreshToken)
    }

    @Transactional
    override fun deleteAllByUserId(userId: Long) {
        jpaRepository.deleteByUserId(userId)
    }
}
