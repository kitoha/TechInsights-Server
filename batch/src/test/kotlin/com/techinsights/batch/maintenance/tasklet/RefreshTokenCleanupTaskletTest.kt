package com.techinsights.batch.maintenance.tasklet

import com.techinsights.domain.service.user.RefreshTokenService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.batch.repeat.RepeatStatus
import java.time.Duration
import java.time.Instant

class RefreshTokenCleanupTaskletTest : FunSpec({
    val refreshTokenService = mockk<RefreshTokenService>()
    val retention = Duration.ofDays(30)
    val tasklet = RefreshTokenCleanupTasklet(refreshTokenService, retention)

    test("만료된 세션을 삭제하고 보존 기간보다 오래된 폐기 해시를 삭제한 뒤 종료한다") {
        val now = slot<Instant>()
        val cutoff = slot<Instant>()
        every { refreshTokenService.deleteExpired(capture(now)) } returns 3
        every { refreshTokenService.deleteRetiredBefore(capture(cutoff)) } returns 5

        val status = tasklet.execute(mockk(relaxed = true), mockk(relaxed = true))

        status shouldBe RepeatStatus.FINISHED
        Duration.between(cutoff.captured, now.captured) shouldBe retention
        now.captured shouldNotBe null
        verify(exactly = 1) { refreshTokenService.deleteExpired(any()) }
        verify(exactly = 1) { refreshTokenService.deleteRetiredBefore(any()) }
    }
})
