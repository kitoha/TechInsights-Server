package com.techinsights.domain.service.quota

import com.techinsights.domain.config.resilience.DailyQuotaProperties
import com.techinsights.domain.repository.quota.DailyQuotaRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class DailyQuotaServiceImplTest : FunSpec({

    val repository = mockk<DailyQuotaRepository>()

    fun service(
        now: String,
        zone: String = "America/Los_Angeles",
        limits: DailyQuotaProperties.Limits = DailyQuotaProperties.Limits()
    ) = DailyQuotaServiceImpl(
        dailyQuotaRepository = repository,
        dailyQuotaProperties = DailyQuotaProperties(zone = zone, limits = limits),
        clock = Clock.fixed(Instant.parse(now), ZoneOffset.UTC)
    )

    test("사용일은 LA 자정 기준이다 - UTC 05시는 전날로 계산된다") {
        every { repository.tryIncrement(any(), any(), any()) } returns true

        service("2026-10-23T05:00:00Z").tryReserve(DailyQuota.BATCH_SUMMARY) shouldBe true

        verify { repository.tryIncrement(LocalDate.parse("2026-10-22"), "geminiBatchRpd", 20) }
    }

    test("LA 자정이 지나면 새 사용일로 계산된다") {
        every { repository.tryIncrement(any(), any(), any()) } returns true

        service("2026-10-23T07:30:00Z").tryReserve(DailyQuota.README_SUMMARY)

        verify { repository.tryIncrement(LocalDate.parse("2026-10-23"), "geminiReadmeRpd", 20) }
    }

    test("zone 설정을 바꿀 수 있다") {
        every { repository.tryIncrement(any(), any(), any()) } returns true

        service("2026-10-23T05:00:00Z", zone = "UTC").tryReserve(DailyQuota.COMMUNITY_ANALYSIS)

        verify { repository.tryIncrement(LocalDate.parse("2026-10-23"), "geminiCommunityRpd", 20) }
    }

    test("한도를 초과하면 false를 반환한다") {
        every { repository.tryIncrement(any(), any(), any()) } returns false

        service("2026-10-23T12:00:00Z").tryReserve(DailyQuota.BATCH_SUMMARY) shouldBe false
    }

    test("쿼터별 한도는 설정값을 사용한다") {
        every { repository.tryIncrement(any(), any(), any()) } returns true
        val limits = DailyQuotaProperties.Limits(batchSummary = 7, readmeSummary = 8, communityAnalysis = 9)
        val quotaService = service("2026-10-23T12:00:00Z", limits = limits)

        quotaService.tryReserve(DailyQuota.BATCH_SUMMARY)
        quotaService.tryReserve(DailyQuota.README_SUMMARY)
        quotaService.tryReserve(DailyQuota.COMMUNITY_ANALYSIS)

        verify { repository.tryIncrement(any(), "geminiBatchRpd", 7) }
        verify { repository.tryIncrement(any(), "geminiReadmeRpd", 8) }
        verify { repository.tryIncrement(any(), "geminiCommunityRpd", 9) }
    }

    test("EMBEDDING 쿼터는 설정된 한도와 geminiEmbeddingRpd 키를 사용한다") {
        every { repository.tryIncrement(any(), any(), any()) } returns true
        val limits = DailyQuotaProperties.Limits(embedding = 11)

        service("2026-10-23T12:00:00Z", limits = limits).tryReserve(DailyQuota.EMBEDDING)

        verify { repository.tryIncrement(any(), "geminiEmbeddingRpd", 11) }
    }

    test("한도가 0 이하면 저장소를 호출하지 않고 false를 반환한다") {
        val limits = DailyQuotaProperties.Limits(batchSummary = 0)

        service("2026-10-23T12:00:00Z", limits = limits).tryReserve(DailyQuota.BATCH_SUMMARY) shouldBe false
    }
})
