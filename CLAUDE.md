# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Module Structure

Multi-module Gradle project with three modules:

- **`api`** — Spring Boot web layer (controllers, filters, auth, security config). Depends on `domain`.
- **`domain`** — Business logic, JPA entities, repositories, services, Flyway migrations, QueryDSL Q-types.
- **`batch`** — Spring Batch jobs for crawling, embedding, summarization, GitHub trending. Depends on `domain`.

## Commands

```bash
# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :api:test
./gradlew :domain:test
./gradlew :batch:test

# Run a single test class
./gradlew :api:test --tests "com.techinsights.api.post.PostControllerTest"

# Build
./gradlew build

# Coverage report (generated at build/reports/jacoco/test/jacocoTestReport.xml)
./gradlew jacocoTestReport
```

## Local Environment

```bash
# Start DB only (PostgreSQL + pgvector)
docker-compose up postgres

# Start full stack (api + batch + nginx + postgres)
docker-compose up
```

Requires a `.env` file at the project root with `DB_PASSWORD` and other secrets. Active Spring profile is `local` — config lives in `application-local.yml` in each module.

## Testing Conventions

- All tests use **Kotest `FunSpec`** style. New tests must follow this style.
- Use **Mockk** for mocking (`mockk<T>()`, `every { } returns`, `verify { }`).
- Tests are fully mocked — no database required to run unit tests.
- Coverage exclusions (enforced by Jacoco + SonarCloud): `config/**`, `dto/**`, `entity/**`, `*Application*.kt`.

## Database Migrations

Flyway migrations live in `domain/src/main/resources/db/migration/`.

- Versioned: `V{N}__{Description}.sql` (e.g., `V015__Drop_Denormalized_Columns.sql`)
- Bootstrap: `B001__Initial_Schema.sql` (runs before versioned migrations)
- Always increment the version number sequentially.

## Key Domain Patterns

- **IDs**: Use `Tsid` (`com.techinsights.domain.utils.Tsid`) for generating entity IDs, not auto-increment.
- **QueryDSL**: Q-types are generated via `kapt`. Run `./gradlew kaptKotlin` if Q-types are missing.
- **Resilience4j**: Circuit breaker and rate limiter are configured in `domain` and used in services calling external APIs.
- **Vector search**: `pgvector` extension with HNSW index (see `V011__Add_Vector_Hnsw_Index.sql`). Embedding stored in `PostEmbedding` and `GithubRepository` entities.

## For module-specific guidance, see AGENTS.md in each module directory.
