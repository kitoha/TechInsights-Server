package com.techinsights.domain.support

import io.kotest.matchers.collections.shouldBeEmpty
import jakarta.persistence.Column
import jakarta.persistence.EntityManager
import jakarta.persistence.Table
import org.springframework.jdbc.core.JdbcTemplate

class SchemaValidationTest(
  private val entityManager: EntityManager,
  private val jdbc: JdbcTemplate
) : RepositoryTestSpec({

  // The Spring context only starts when Hibernate's ddl-auto=validate accepts the Flyway schema
  // (column existence and types). validate does not compare VARCHAR lengths, so check them here.
  test("no entity column is longer than its database column") {
    val tooNarrow = mutableListOf<String>()

    entityManager.metamodel.entities.map { it.javaType }.forEach { type ->
      val table = type.getAnnotation(Table::class.java)?.name ?: return@forEach
      generateSequence<Class<*>>(type) { it.superclass }
        .flatMap { it.declaredFields.asSequence() }
        .mapNotNull { field -> field.getAnnotation(Column::class.java)?.let { field.name to it } }
        .filter { (_, column) -> column.name.isNotEmpty() && column.length != 255 }
        .forEach { (fieldName, column) ->
          val dbLength = jdbc.queryForList(
            """
            SELECT character_maximum_length FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
              AND character_maximum_length IS NOT NULL
            """.trimIndent(),
            Int::class.java, table, column.name
          ).firstOrNull()
          if (dbLength != null && dbLength < column.length) {
            tooNarrow += "$table.${column.name} ($fieldName): entity ${column.length} > db $dbLength"
          }
        }
    }

    tooNarrow.shouldBeEmpty()
  }
})
