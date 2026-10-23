package com.techinsights.domain.service.user

import com.techinsights.domain.dto.user.RefreshTokenRotationResult
import com.techinsights.domain.entity.user.RefreshToken
import com.techinsights.domain.repository.user.RefreshTokenRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import java.time.LocalDateTime
import java.util.Optional

class RefreshTokenServiceTest : FunSpec({
    val repository = mockk<RefreshTokenRepository>()
    val service = RefreshTokenService(repository)
    val newExpiry = Instant.now().plusSeconds(3600)

    fun token(
        tokenHash: String = "current",
        previousTokenHash: String? = null,
        expiryAt: Instant = Instant.now().plusSeconds(3600),
        updatedAt: LocalDateTime = LocalDateTime.now()
    ) = RefreshToken(
        id = 1L,
        userId = 100L,
        tokenHash = tokenHash,
        previousTokenHash = previousTokenHash,
        deviceId = "login-state-device",
        expiryAt = expiryAt
    ).apply { this.updatedAt = updatedAt }

    beforeTest {
        clearMocks(repository)
        every { repository.save(any()) } answers { firstArg() }
        every { repository.delete(any()) } just runs
    }

    test("현재 해시와 일치하는 행을 잠근 채로 그 행을 회전해야 한다") {
        val row = token(tokenHash = "presented")
        every { repository.findByHashForUpdate("presented") } returns Optional.of(row)

        val result = service.rotate("presented", "new", newExpiry)

        result shouldBe RefreshTokenRotationResult.Rotated
        row.tokenHash shouldBe "new"
        row.previousTokenHash shouldBe "presented"
        row.expiryAt shouldBe newExpiry
        verify { repository.save(row) }
    }

    test("만료된 현재 토큰은 삭제하고 Expired를 반환해야 한다") {
        val row = token(tokenHash = "presented", expiryAt = Instant.now().minusSeconds(1))
        every { repository.findByHashForUpdate("presented") } returns Optional.of(row)

        val result = service.rotate("presented", "new", newExpiry)

        result shouldBe RefreshTokenRotationResult.Expired
        verify { repository.delete(row) }
        verify(exactly = 0) { repository.save(any()) }
    }

    test("유예 시간 내에 직전 해시가 제시되면 같은 행을 다시 회전해야 한다") {
        val row = token(tokenHash = "winner", previousTokenHash = "presented", updatedAt = LocalDateTime.now())
        every { repository.findByHashForUpdate("presented") } returns Optional.empty()
        every { repository.findByPreviousHashForUpdate("presented") } returns Optional.of(row)

        val result = service.rotate("presented", "new", newExpiry)

        result shouldBe RefreshTokenRotationResult.Rotated
        row.tokenHash shouldBe "new"
        row.previousTokenHash shouldBe "winner"
        verify { repository.save(row) }
    }

    test("유예 시간이 지난 뒤 직전 해시가 제시되면 ReuseDetected를 반환하고 행을 바꾸지 않아야 한다") {
        val row = token(tokenHash = "winner", previousTokenHash = "presented", updatedAt = LocalDateTime.now().minusMinutes(5))
        every { repository.findByHashForUpdate("presented") } returns Optional.empty()
        every { repository.findByPreviousHashForUpdate("presented") } returns Optional.of(row)

        val result = service.rotate("presented", "new", newExpiry)

        result shouldBe RefreshTokenRotationResult.ReuseDetected
        row.tokenHash shouldBe "winner"
        verify(exactly = 0) { repository.save(any()) }
    }

    test("현재 해시도 직전 해시도 아니면 NotFound를 반환해야 한다") {
        every { repository.findByHashForUpdate("unknown") } returns Optional.empty()
        every { repository.findByPreviousHashForUpdate("unknown") } returns Optional.empty()

        val result = service.rotate("unknown", "new", newExpiry)

        result shouldBe RefreshTokenRotationResult.NotFound
        verify(exactly = 0) { repository.save(any()) }
        verify(exactly = 0) { repository.delete(any()) }
    }

    test("유예 시간 내라도 만료된 행은 회전하지 않고 삭제해야 한다") {
        val row = token(
            tokenHash = "winner",
            previousTokenHash = "presented",
            expiryAt = Instant.now().minusSeconds(1)
        )
        every { repository.findByHashForUpdate("presented") } returns Optional.empty()
        every { repository.findByPreviousHashForUpdate("presented") } returns Optional.of(row)

        val result = service.rotate("presented", "new", newExpiry)

        result shouldBe RefreshTokenRotationResult.Expired
        verify { repository.delete(row) }
    }

    context("upsertToken (login)") {
        test("같은 기기의 기존 행을 교체할 때 previous_token_hash는 null이어야 한다") {
            val row = token(tokenHash = "old", previousTokenHash = "older")
            every { repository.findByUserAndDevice(100L, "dev") } returns Optional.of(row)

            service.upsertToken(100L, "dev", "login-hash", newExpiry)

            row.tokenHash shouldBe "login-hash"
            row.previousTokenHash shouldBe null
            row.expiryAt shouldBe newExpiry
            verify { repository.save(row) }
        }

        test("기기 ID가 있고 기존 행이 없으면 새 행을 만들어야 한다") {
            every { repository.findByUserAndDevice(100L, "dev") } returns Optional.empty()
            val saved = slot<RefreshToken>()
            every { repository.save(capture(saved)) } answers { saved.captured }

            service.upsertToken(100L, "dev", "login-hash", newExpiry)

            saved.captured.userId shouldBe 100L
            saved.captured.deviceId shouldBe "dev"
            saved.captured.tokenHash shouldBe "login-hash"
            saved.captured.previousTokenHash shouldBe null
        }

        test("기기 ID가 없으면 기존 행을 조회하지 않고 항상 새 세션 행을 만들어야 한다") {
            val saved = slot<RefreshToken>()
            every { repository.save(capture(saved)) } answers { saved.captured }

            service.upsertToken(100L, null, "login-hash", newExpiry)

            saved.captured.deviceId shouldBe null
            saved.captured.tokenHash shouldBe "login-hash"
            verify(exactly = 0) { repository.findByUserAndDevice(any(), any()) }
        }
    }

    context("deleteByHash (logout)") {
        test("현재 해시와 일치하는 행을 삭제해야 한다") {
            val row = token(tokenHash = "presented")
            every { repository.findByHashForUpdate("presented") } returns Optional.of(row)

            service.deleteByHash("presented")

            verify { repository.delete(row) }
        }

        test("유예 시간 내 직전 해시와 일치하는 행도 삭제해야 한다") {
            val row = token(tokenHash = "winner", previousTokenHash = "presented", updatedAt = LocalDateTime.now())
            every { repository.findByHashForUpdate("presented") } returns Optional.empty()
            every { repository.findByPreviousHashForUpdate("presented") } returns Optional.of(row)

            service.deleteByHash("presented")

            verify { repository.delete(row) }
        }

        test("유예 시간이 지난 직전 해시로는 행을 삭제하지 않아야 한다") {
            val row = token(tokenHash = "winner", previousTokenHash = "presented", updatedAt = LocalDateTime.now().minusMinutes(5))
            every { repository.findByHashForUpdate("presented") } returns Optional.empty()
            every { repository.findByPreviousHashForUpdate("presented") } returns Optional.of(row)

            service.deleteByHash("presented")

            verify(exactly = 0) { repository.delete(any()) }
        }

        test("일치하는 행이 없으면 아무것도 삭제하지 않아야 한다") {
            every { repository.findByHashForUpdate("unknown") } returns Optional.empty()
            every { repository.findByPreviousHashForUpdate("unknown") } returns Optional.empty()

            service.deleteByHash("unknown")

            verify(exactly = 0) { repository.delete(any()) }
        }
    }
})
