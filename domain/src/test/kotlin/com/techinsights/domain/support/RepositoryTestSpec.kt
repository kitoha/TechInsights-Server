package com.techinsights.domain.support

import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * Base for repository / SQL tests against real PostgreSQL + pgvector with the real Flyway migrations.
 *
 * Data is isolated by truncating every table before each test rather than rolling back a
 * transaction: repository methods open their own transactions, and tests of concurrency or of
 * native SQL need real commits.
 */
@SpringBootTest(classes = [RepositoryTestApplication::class])
abstract class RepositoryTestSpec(body: FunSpec.() -> Unit = {}) : FunSpec() {

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  init {
    extensions(SpringExtension)
    beforeEach { truncateAllTables() }
    body()
  }

  private fun truncateAllTables() {
    val tables = jdbcTemplate.queryForList(
      "SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'",
      String::class.java
    )
    jdbcTemplate.execute("TRUNCATE TABLE ${tables.joinToString(", ")} RESTART IDENTITY CASCADE")
  }

  companion object {
    @JvmStatic
    @DynamicPropertySource
    fun datasourceProperties(registry: DynamicPropertyRegistry) {
      val container = PostgresTestContainer.instance
      registry.add("spring.datasource.url", container::getJdbcUrl)
      registry.add("spring.datasource.username", container::getUsername)
      registry.add("spring.datasource.password", container::getPassword)
    }
  }
}
