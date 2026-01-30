package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.User
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository
) : UserRepository {
    override fun findByGoogleSub(googleSub: String): Optional<User> {
        return userJpaRepository.findByGoogleSub(googleSub)
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
}
