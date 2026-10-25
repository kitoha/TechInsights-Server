package com.techinsights.domain.repository.post

import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.utils.Tsid
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import java.time.LocalDateTime

class PostRepositoryInsertTest : FunSpec({

  val entityManager = mockk<EntityManager>()
  val query = mockk<Query>()
  val repository = PostRepositoryImpl(mockk<PostJpaRepository>(), mockk<JPAQueryFactory>(), entityManager)

  fun post(title: String, url: String) = PostDto(
    id = Tsid.encode(1L),
    title = title,
    preview = null,
    url = url,
    content = "content",
    publishedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
    company = CompanyDto(Tsid.encode(1L), "company", "https://blog.example.com", "logo.png"),
    categories = emptySet()
  )

  test("insertAllIgnoringDuplicates truncates title and url to the column lengths so one row cannot fail the chunk") {
    val parameters = mutableMapOf<String, Any?>()
    every { entityManager.createNativeQuery(any<String>()) } returns query
    every { query.setParameter(any<String>(), any()) } answers {
      parameters[firstArg()] = secondArg()
      query
    }
    every { query.executeUpdate() } returns 1

    repository.insertAllIgnoringDuplicates(listOf(post("t".repeat(600), "https://a.com/" + "u".repeat(3000))))

    (parameters["title"] as String).length shouldBe 500
    (parameters["url"] as String).length shouldBe 2048
  }
})
