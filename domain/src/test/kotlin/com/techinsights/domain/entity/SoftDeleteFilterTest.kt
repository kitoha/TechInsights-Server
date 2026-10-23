package com.techinsights.domain.entity

import com.techinsights.domain.entity.company.Company
import com.techinsights.domain.entity.github.GithubBookmark
import com.techinsights.domain.entity.github.GithubRepository
import com.techinsights.domain.entity.post.Post
import com.techinsights.domain.entity.post.PostBookmark
import com.techinsights.domain.entity.post.PostEmbedding
import com.techinsights.domain.entity.post.PostLike
import com.techinsights.domain.entity.post.PostView
import com.techinsights.domain.entity.user.AnonymousUserReadHistory
import com.techinsights.domain.entity.user.RefreshToken
import com.techinsights.domain.entity.user.User
import com.techinsights.domain.entity.user.UserReadHistory
import com.techinsights.domain.repository.company.CompanyJpaRepository
import com.techinsights.domain.repository.post.PostJpaRepository
import com.techinsights.domain.repository.user.UserReadHistoryJpaRepository
import com.techinsights.domain.repository.user.UserJpaRepository
import com.techinsights.domain.support.RepositoryTestSpec
import com.techinsights.domain.support.SqlFixtures
import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import kotlin.reflect.KClass

/**
 * `deleted_at IS NULL` must hide soft-deleted rows from every normal read (JPQL, derived queries,
 * `findById`/`em.find`). Every row seeded here is soft-deleted; live rows must still be visible.
 */
class SoftDeleteFilterTest : RepositoryTestSpec() {

  @Autowired lateinit var entityManager: EntityManager
  @Autowired lateinit var userJpaRepository: UserJpaRepository
  @Autowired lateinit var postJpaRepository: PostJpaRepository
  @Autowired lateinit var companyJpaRepository: CompanyJpaRepository
  @Autowired lateinit var userReadHistoryJpaRepository: UserReadHistoryJpaRepository
  @Autowired lateinit var transactionManager: PlatformTransactionManager

  private val deletedAt = "TIMESTAMP '2024-01-01 00:00:00'"

  private fun seedSoftDeletedRows() {
    val fixtures = SqlFixtures(jdbcTemplate)
    // live parents (id 1) that the soft-deleted children point to
    fixtures.insertCompany(1L)
    fixtures.insertPost(1L)
    fixtures.insertUser(1L)
    fixtures.insertGithubRepo(1L)
    // soft-deleted rows (id 2 for parents, id 10 for children)
    fixtures.insertCompany(2L)
    fixtures.insertPost(2L, companyId = 1L)
    fixtures.insertUser(2L)
    fixtures.insertGithubRepo(2L)
    jdbcTemplate.update("UPDATE company SET deleted_at = $deletedAt WHERE id = 2")
    jdbcTemplate.update("UPDATE posts SET deleted_at = $deletedAt WHERE id = 2")
    jdbcTemplate.update("UPDATE users SET deleted_at = $deletedAt, nickname = 'gone' WHERE id = 2")
    jdbcTemplate.update("UPDATE github_repositories SET deleted_at = $deletedAt WHERE id = 2")

    jdbcTemplate.update(
      "INSERT INTO post_likes (id, post_id, user_id, ip_address, created_at, updated_at, deleted_at) " +
        "VALUES (10, 1, 1, 'ip', now(), now(), $deletedAt)"
    )
    jdbcTemplate.update(
      "INSERT INTO post_bookmarks (id, post_id, user_id, created_at, updated_at, deleted_at) " +
        "VALUES (10, 1, 1, now(), now(), $deletedAt)"
    )
    jdbcTemplate.update(
      "INSERT INTO post_view (id, post_id, user_or_ip, viewed_date, created_at, updated_at, deleted_at) " +
        "VALUES (10, 1, 'ip', now(), now(), now(), $deletedAt)"
    )
    jdbcTemplate.update(
      "INSERT INTO post_embedding (post_id, company_name, categories, content, embedding_vector, created_at, updated_at, deleted_at) " +
        "VALUES (1, 'c', 'c', 'x', CAST(? AS vector), now(), now(), $deletedAt)",
      fixtures.vectorLiteral(1f)
    )
    jdbcTemplate.update(
      "INSERT INTO anonymous_user_read_history (id, anonymous_id, post_id, read_at, created_at, updated_at, deleted_at) " +
        "VALUES (10, 'anon', 1, now(), now(), now(), $deletedAt)"
    )
    jdbcTemplate.update(
      "INSERT INTO user_read_history (id, user_id, post_id, read_at, created_at, updated_at, deleted_at) " +
        "VALUES (10, 1, 1, now(), now(), now(), $deletedAt)"
    )
    jdbcTemplate.update(
      "INSERT INTO refresh_tokens (id, user_id, family_id, token_hash, expiry_at, created_at, updated_at, deleted_at) " +
        "VALUES (10, 1, 10, 'hash', now(), now(), now(), $deletedAt)"
    )
    jdbcTemplate.update(
      "INSERT INTO github_bookmarks (id, repo_id, user_id, created_at, updated_at, deleted_at) " +
        "VALUES (10, 1, 1, now(), now(), $deletedAt)"
    )
  }

  private fun visibleRowCount(entity: KClass<*>): Int =
    entityManager.createQuery("SELECT e FROM ${entity.simpleName} e", entity.java).resultList.size

  init {
    // parents: one live row (id 1) and one soft-deleted row (id 2); children: soft-deleted only
    val parents = listOf(Company::class, Post::class, User::class, GithubRepository::class)
    val children = listOf(
      PostLike::class, PostBookmark::class, PostView::class, PostEmbedding::class,
      AnonymousUserReadHistory::class, UserReadHistory::class, RefreshToken::class, GithubBookmark::class
    )

    parents.forEach { entity ->
      test("JPQL은 소프트 삭제된 ${entity.simpleName}를 반환하지 않는다") {
        seedSoftDeletedRows()

        visibleRowCount(entity) shouldBe 1
      }
    }

    children.forEach { entity ->
      test("JPQL은 소프트 삭제된 ${entity.simpleName}를 반환하지 않는다") {
        seedSoftDeletedRows()

        visibleRowCount(entity) shouldBe 0
      }
    }

    test("파생 쿼리 findByNickname은 소프트 삭제된 사용자를 반환하지 않는다") {
      seedSoftDeletedRows()

      userJpaRepository.findByNickname("gone").isPresent shouldBe false
    }

    test("findById와 em.find는 소프트 삭제된 행을 반환하지 않는다") {
      seedSoftDeletedRows()

      userJpaRepository.findById(2L).isPresent shouldBe false
      postJpaRepository.findById(2L).isPresent shouldBe false
      companyJpaRepository.findById(2L).isPresent shouldBe false
      entityManager.find(User::class.java, 2L) shouldBe null
      userJpaRepository.findAllById(listOf(1L, 2L)).map { it.id } shouldBe listOf(1L)
    }

    test("소프트 삭제 행은 count와 existsById에서도 제외된다") {
      seedSoftDeletedRows()

      userJpaRepository.count() shouldBe 1L
      userJpaRepository.existsById(2L) shouldBe false
      companyJpaRepository.count() shouldBe 1L
    }

    test("삭제되지 않은 행은 계속 조회된다") {
      seedSoftDeletedRows()

      userJpaRepository.findById(1L).isPresent shouldBe true
      postJpaRepository.findById(1L).isPresent shouldBe true
    }

    test("읽은 글 upsert는 소프트 삭제된 행을 되살린다") {
      seedSoftDeletedRows()

      TransactionTemplate(transactionManager).execute {
        userReadHistoryJpaRepository.upsertRead(11L, 1L, 1L, LocalDateTime.of(2024, 5, 1, 0, 0))
      }

      visibleRowCount(UserReadHistory::class) shouldBe 1
      jdbcTemplate.queryForObject("SELECT count(*) FROM user_read_history WHERE deleted_at IS NOT NULL", Int::class.java) shouldBe 0
    }

    test("소프트 삭제된 회사의 게시글은 join fetch 조회와 findById에서도 보이지 않는다") {
      val fixtures = SqlFixtures(jdbcTemplate)
      fixtures.insertCompany(5L)
      fixtures.insertPost(50L, companyId = 5L)
      jdbcTemplate.update("UPDATE company SET deleted_at = $deletedAt WHERE id = 5")

      entityManager.createQuery("SELECT p FROM Post p JOIN FETCH p.company", Post::class.java)
        .resultList.size shouldBe 0
      postJpaRepository.findById(50L).isPresent shouldBe false
    }
  }
}
