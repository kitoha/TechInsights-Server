package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.ProviderType
import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.support.SqlFixtures
import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

class UserRepositoryTest(
  private val repository: UserRepository,
  private val jdbc: JdbcTemplate
) : RepositoryTestSpec({

  val fixtures = SqlFixtures(jdbc)

  fun seed(id: Long, nickname: String, deleted: Boolean = false) =
    fixtures.insertUser(id, nickname, LocalDateTime.of(2024, 1, 1, 0, 0), deleted)

  test("finds an active user by id, email, provider and nickname") {
    seed(1L, "alice1")
    jdbc.update("UPDATE users SET provider = 'GOOGLE', provider_id = 'google-1' WHERE id = 1")

    repository.findById(1L).get().nickname shouldBe "alice1"
    repository.findByEmail("user1@example.com").get().id shouldBe 1L
    repository.findByProviderAndProviderId(ProviderType.GOOGLE, "google-1").get().id shouldBe 1L
    repository.findByNickname("alice1").get().id shouldBe 1L
  }

  test("unknown lookups are empty") {
    repository.findById(404L).isPresent shouldBe false
    repository.findByEmail("none@example.com").isPresent shouldBe false
    repository.findByNickname("nobody").isPresent shouldBe false
  }

  test("soft-deleted users are invisible to every lookup") {
    seed(1L, "ghost33", deleted = true)
    jdbc.update("UPDATE users SET provider = 'GOOGLE', provider_id = 'google-1' WHERE id = 1")

    repository.findById(1L).isPresent shouldBe false
    repository.findByEmail("user1@example.com").isPresent shouldBe false
    repository.findByProviderAndProviderId(ProviderType.GOOGLE, "google-1").isPresent shouldBe false
    repository.findByNickname("ghost33").isPresent shouldBe false
  }

  test("save inserts a new user and persists later changes") {
    val user = User(
      id = 7L, email = "new@example.com", name = "New", nickname = "newbie7",
      provider = ProviderType.GOOGLE, providerId = "google-7"
    )

    repository.save(user)
    val loaded = repository.findById(7L).get()
    loaded.name = "Renamed"
    repository.save(loaded)

    jdbc.queryForObject("SELECT name FROM users WHERE id = 7", String::class.java) shouldBe "Renamed"
    jdbc.queryForObject("SELECT count(*) FROM users", Int::class.java) shouldBe 1
  }
})
