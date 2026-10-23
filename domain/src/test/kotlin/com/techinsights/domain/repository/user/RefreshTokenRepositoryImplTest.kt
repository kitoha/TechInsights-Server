package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.RefreshToken
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.util.*

class RefreshTokenRepositoryImplTest : FunSpec({
    val jpaRepository = mockk<RefreshTokenJpaRepository>()
    val repository = RefreshTokenRepositoryImpl(jpaRepository)

    val refreshToken = RefreshToken(
        id = 1L,
        userId = 100L,
        tokenHash = "hash-123",
        deviceId = "device-1",
        expiryAt = Instant.now().plusSeconds(3600)
    )

    beforeTest {
        clearAllMocks()
    }

    test("사용자 ID와 기기 ID로 리프레시 토큰을 찾을 수 있어야 한다") {
        every { jpaRepository.findByUserIdAndDeviceId(100L, "device-1") } returns Optional.of(refreshToken)

        val result = repository.findByUserAndDevice(100L, "device-1")

        result.isPresent shouldBe true
        result.get().tokenHash shouldBe "hash-123"
        verify(exactly = 1) { jpaRepository.findByUserIdAndDeviceId(100L, "device-1") }
    }

    test("리프레시 토큰을 저장할 수 있어야 한다") {
        every { jpaRepository.save(any()) } returns refreshToken

        val result = repository.save(refreshToken)

        result.id shouldBe 1L
        verify(exactly = 1) { jpaRepository.save(refreshToken) }
    }

    test("해시값으로 잠금 조회할 수 있어야 한다") {
        every { jpaRepository.findByTokenHashForUpdate("hash-123") } returns Optional.of(refreshToken)

        val result = repository.findByHashForUpdate("hash-123")

        result.get().userId shouldBe 100L
        verify(exactly = 1) { jpaRepository.findByTokenHashForUpdate("hash-123") }
    }

    test("이전 해시값으로 잠금 조회할 수 있어야 한다") {
        every { jpaRepository.findByPreviousTokenHashForUpdate("prev-123") } returns Optional.of(refreshToken)

        val result = repository.findByPreviousHashForUpdate("prev-123")

        result.get().userId shouldBe 100L
        verify(exactly = 1) { jpaRepository.findByPreviousTokenHashForUpdate("prev-123") }
    }
})
