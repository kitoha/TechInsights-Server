package com.techinsights.domain.service.post

import com.techinsights.domain.config.AsyncConfig
import com.techinsights.domain.event.ViewCountEventHandler
import com.techinsights.domain.service.company.CompanyViewCountUpdaterImpl
import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.support.SqlFixtures
import com.techinsights.domain.utils.Tsid
import io.kotest.matchers.shouldBe
import org.awaitility.Awaitility.await
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@Import(
  PostViewService::class,
  RdbViewCountUpdater::class,
  CompanyViewCountUpdaterImpl::class,
  ViewCountEventHandler::class,
  AsyncConfig::class
)
class PostViewRecordingTest(
  private val service: PostViewService,
  private val jdbc: JdbcTemplate
) : RepositoryTestSpec({

  val fixtures = SqlFixtures(jdbc)
  val postId = Tsid.encode(1L)

  fun viewRows(): Int = jdbc.queryForObject("SELECT count(*) FROM post_view", Int::class.java)!!
  fun postViewCount(): Long = jdbc.queryForObject("SELECT view_count FROM posts WHERE id = 1", Long::class.java)!!
  fun companyViewCount(): Long = jdbc.queryForObject("SELECT total_view_count FROM company WHERE id = 1", Long::class.java)!!

  beforeEach {
    fixtures.insertCompany(1L)
    fixtures.insertPost(1L)
  }

  test("a repeated view by the same identifier on the same day is stored and counted once") {
    repeat(3) { service.recordView(postId, "aid-1", "agent") }
    service.recordView(postId, "aid-2", "agent")

    await().atMost(Duration.ofSeconds(5)).untilAsserted { postViewCount() shouldBe 2L }
    Thread.sleep(300) // a wrongly published extra event would have landed by now
    postViewCount() shouldBe 2L
    companyViewCount() shouldBe 2L
    viewRows() shouldBe 2
  }

  test("concurrent views by the same identifier are stored and counted once") {
    val threads = 8
    val executor = Executors.newFixedThreadPool(threads)
    val start = CountDownLatch(1)
    val futures = (1..threads).map {
      executor.submit {
        start.await()
        service.recordView(postId, "aid-race", "agent")
      }
    }
    start.countDown()
    futures.forEach { it.get() }
    executor.shutdown()

    await().atMost(Duration.ofSeconds(5)).untilAsserted { postViewCount() shouldBe 1L }
    Thread.sleep(300)
    postViewCount() shouldBe 1L
    viewRows() shouldBe 1
  }
})
