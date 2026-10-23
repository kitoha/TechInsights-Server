package com.techinsights.domain.repository.quota

import java.time.LocalDate

interface DailyQuotaRepository {
  /**
   * 해당 일자·키의 카운터를 원자적으로 1 증가시킨다. 이미 [limit]에 도달했다면 증가시키지 않고 false를 반환한다.
   * 호출자의 트랜잭션과 무관하게 즉시 커밋된다.
   */
  fun tryIncrement(usageDate: LocalDate, quotaKey: String, limit: Int): Boolean
}
