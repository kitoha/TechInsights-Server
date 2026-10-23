package com.techinsights.domain.service.quota

import com.techinsights.domain.config.resilience.DailyQuotaProperties
import com.techinsights.domain.config.resilience.RateLimiterProperties
import com.techinsights.domain.repository.quota.DailyQuotaRepository
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@Service
class DailyQuotaServiceImpl(
  private val dailyQuotaRepository: DailyQuotaRepository,
  private val rateLimiterProperties: RateLimiterProperties,
  dailyQuotaProperties: DailyQuotaProperties,
  private val clock: Clock = Clock.systemUTC()
) : DailyQuotaService {

  private val zone = ZoneId.of(dailyQuotaProperties.zone)

  override fun tryReserve(quota: DailyQuota): Boolean {
    val limit = limitOf(quota)
    if (limit <= 0) return false
    return dailyQuotaRepository.tryIncrement(LocalDate.now(clock.withZone(zone)), quota.key, limit)
  }

  private fun limitOf(quota: DailyQuota): Int = when (quota) {
    DailyQuota.BATCH_SUMMARY -> rateLimiterProperties.geminiBatchRpd.limitForPeriod
    DailyQuota.README_SUMMARY -> rateLimiterProperties.geminiReadmeRpd.limitForPeriod
    DailyQuota.COMMUNITY_ANALYSIS -> rateLimiterProperties.geminiCommunityRpd.limitForPeriod
  }
}
