package com.techinsights.domain.support

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/** One pgvector container shared by every spec in the test JVM; Ryuk removes it on exit. */
object PostgresTestContainer {

  val instance: PostgreSQLContainer<*> by lazy {
    PostgreSQLContainer(
      DockerImageName.parse("pgvector/pgvector:pg15").asCompatibleSubstituteFor("postgres")
    ).apply { start() }
  }
}
