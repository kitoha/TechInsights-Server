package com.techinsights.domain.repository.quota

import org.springframework.stereotype.Repository
import java.sql.Date
import javax.sql.DataSource
import java.time.LocalDate

@Repository
class DailyQuotaRepositoryImpl(
  private val dataSource: DataSource
) : DailyQuotaRepository {

  override fun tryIncrement(usageDate: LocalDate, quotaKey: String, limit: Int): Boolean {
    dataSource.connection.use { connection ->
      connection.prepareStatement(UPSERT_SQL).use { statement ->
        statement.setDate(1, Date.valueOf(usageDate))
        statement.setString(2, quotaKey)
        statement.setInt(3, limit)
        statement.executeQuery().use { resultSet -> return resultSet.next() }
      }
    }
  }

  companion object {
    private const val UPSERT_SQL = """
      INSERT INTO gemini_daily_usage (usage_date, quota_key, request_count, updated_at)
      VALUES (?, ?, 1, now())
      ON CONFLICT (usage_date, quota_key) DO UPDATE
        SET request_count = gemini_daily_usage.request_count + 1,
            updated_at = now()
        WHERE gemini_daily_usage.request_count < ?
      RETURNING request_count
    """
  }
}
