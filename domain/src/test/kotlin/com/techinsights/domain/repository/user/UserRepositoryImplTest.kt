package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.ProviderType
import com.techinsights.domain.enums.UserRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.*

class UserRepositoryImplTest : FunSpec({
    val jpaRepository = mockk<UserJpaRepository>()
    val userRepository = UserRepositoryImpl(jpaRepository)

    val testUser = User(
        id = 1L,
        email = "test@example.com",
        name = "Test User",
        nickname = "testuser",
        provider = ProviderType.GOOGLE,
        providerId = "google-123",
        role = UserRole.USER
    )

    beforeTest {
        clearAllMocks()
    }

    test("provider와 providerId로 사용자를 찾을 수 있어야 한다") {
        every { jpaRepository.findByProviderAndProviderId(ProviderType.GOOGLE, "google-123") } returns Optional.of(testUser)

        val result = userRepository.findByProviderAndProviderId(ProviderType.GOOGLE, "google-123")

        result.isPresent shouldBe true
        result.get().email shouldBe "test@example.com"
        verify(exactly = 1) { jpaRepository.findByProviderAndProviderId(ProviderType.GOOGLE, "google-123") }
    }

    test("email로 사용자를 찾을 수 있어야 한다") {
        every { jpaRepository.findByEmail("test@example.com") } returns Optional.of(testUser)

        val result = userRepository.findByEmail("test@example.com")

        result.isPresent shouldBe true
        result.get().providerId shouldBe "google-123"
        verify(exactly = 1) { jpaRepository.findByEmail("test@example.com") }
    }

    test("사용자 정보를 저장할 수 있어야 한다") {
        every { jpaRepository.save(any()) } returns testUser

        val result = userRepository.save(testUser)

        result.id shouldBe 1L
        verify(exactly = 1) { jpaRepository.save(testUser) }
    }
})
