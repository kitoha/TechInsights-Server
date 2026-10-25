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
import org.springframework.beans.factory.annotation.Value
import java.time.Duration
import java.time.Instant

class RefreshTokenCleanupTaskletTest : FunSpec({
    val refreshTokenService = mockk<RefreshTokenService>()
    val refreshTokenLifetime = Duration.ofDays(30)
    val tasklet = RefreshTokenCleanupTasklet(refreshTokenService, refreshTokenLifetime)

    test("만료된 세션을 삭제하고 보존 기간보다 오래된 폐기 해시를 삭제한 뒤 종료한다") {
        val now = slot<Instant>()
        val cutoff = slot<Instant>()
        every { refreshTokenService.deleteExpired(capture(now)) } returns 3
        every { refreshTokenService.clearExpiredSuccessors(any()) } returns 2
        every { refreshTokenService.deleteRetiredBefore(capture(cutoff)) } returns 5

        val status = tasklet.execute(mockk(relaxed = true), mockk(relaxed = true))

        status shouldBe RepeatStatus.FINISHED
        Duration.between(cutoff.captured, now.captured) shouldBe refreshTokenLifetime
        now.captured shouldNotBe null
        verify(exactly = 1) { refreshTokenService.deleteExpired(any()) }
        verify(exactly = 1) { refreshTokenService.clearExpiredSuccessors(any()) }
        verify(exactly = 1) { refreshTokenService.deleteRetiredBefore(any()) }
    }

    test("폐기 해시 보존 기간은 api 와 같은 프로퍼티(auth.jwt.refresh-token-expiration)에서 가져온다 (따로 설정할 수 없다)") {
        val lifetimeParameter = RefreshTokenCleanupTasklet::class.java.constructors.single().parameters[1]

        lifetimeParameter.getAnnotation(Value::class.java).value shouldBe "\${auth.jwt.refresh-token-expiration:30d}"
    }
})
