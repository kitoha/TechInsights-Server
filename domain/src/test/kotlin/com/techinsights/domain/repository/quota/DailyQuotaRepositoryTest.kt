package com.techinsights.domain.repository.quota

import com.techinsights.domain.support.RepositoryTestSpec
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class DailyQuotaRepositoryTest(
  private val repository: DailyQuotaRepository,
  private val jdbc: JdbcTemplate
) : RepositoryTestSpec({

  val today = LocalDate.of(2024, 1, 1)

  fun usedCount(key: String): Int =
    jdbc.queryForObject("SELECT request_count FROM gemini_daily_usage WHERE quota_key = ?", Int::class.java, key)!!

  test("tryIncrement grants exactly the limit sequentially") {
    val results = (1..5).map { repository.tryIncrement(today, "BATCH_SUMMARY", 3) }

    results shouldBe listOf(true, true, true, false, false)
    usedCount("BATCH_SUMMARY") shouldBe 3
  }

  test("tryIncrement never exceeds the limit under concurrent reservations") {
    val limit = 5
    val threads = 16
    val executor = Executors.newFixedThreadPool(threads)
    val start = CountDownLatch(1)
    val futures = (1..threads).map {
      executor.submit<Boolean> {
        start.await()
        repository.tryIncrement(today, "BATCH_SUMMARY", limit)
      }
    }
    start.countDown()
    val granted = futures.count { it.get() }
    executor.shutdown()

    granted shouldBe limit
    usedCount("BATCH_SUMMARY") shouldBe limit
  }

  test("tryIncrement counts each day and key separately") {
    repository.tryIncrement(today, "BATCH_SUMMARY", 1) shouldBe true
    repository.tryIncrement(today, "BATCH_SUMMARY", 1) shouldBe false
    repository.tryIncrement(today.plusDays(1), "BATCH_SUMMARY", 1) shouldBe true
    repository.tryIncrement(today, "README_SUMMARY", 1) shouldBe true
  }
})
