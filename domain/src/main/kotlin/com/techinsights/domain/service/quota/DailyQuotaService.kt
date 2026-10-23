package com.techinsights.domain.service.quota

fun interface DailyQuotaService {
  /** Gemini 호출 전에 일일 쿼터 1건을 예약한다. false면 한도를 초과한 것이므로 호출하지 않는다. */
  fun tryReserve(quota: DailyQuota): Boolean
}
