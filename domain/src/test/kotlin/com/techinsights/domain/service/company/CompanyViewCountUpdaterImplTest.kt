package com.techinsights.domain.service.company

import com.querydsl.jpa.impl.JPAQueryFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jakarta.persistence.EntityManager
import jakarta.persistence.Query

class CompanyViewCountUpdaterImplTest : FunSpec({

  val entityManager = mockk<EntityManager>()
  val updater = CompanyViewCountUpdaterImpl(mockk<JPAQueryFactory>(), entityManager)

  test("reconcilePostCounts should recompute post_count from summarized, non-deleted posts") {
    val sql = slot<String>()
    val query = mockk<Query>()
    every { entityManager.createNativeQuery(capture(sql)) } returns query
    every { query.executeUpdate() } returns 4

    updater.reconcilePostCounts() shouldBe 4

    sql.captured.contains("UPDATE company") shouldBe true
    sql.captured.contains("is_summary = true") shouldBe true
    sql.captured.contains("deleted_at IS NULL") shouldBe true
  }
})
