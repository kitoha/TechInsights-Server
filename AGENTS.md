# AGENTS.md

This file provides shared guidance for all AI coding assistants (Claude Code, Codex, Gemini, etc.) working in this repository.

## How to Work

- State assumptions before implementing; if multiple interpretations exist, ask.
- If something is unclear, stop and ask — don't pick silently.
- Every changed line must trace directly to the request — don't improve adjacent code.

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

## Git Conventions

- Commit messages must follow Conventional Commits: `<type>(<scope>): <subject>`
- Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`
- Subject: lowercase, imperative mood, no period

## Kotlin Code Style

- Never use fully qualified class names inline — always add an `import` statement.

## For module-specific guidance, see AGENTS.md in each module directory.

## Available Skills & Agents

Use these at the right moment — do not skip them.

| Situation | What to use |
|---|---|
| Planning a new feature or clarifying requirements | `/pm` skill |
| Deciding module placement or design patterns | `architect` agent or `/architecture` skill |
| Adding API endpoints or changing auth logic | `security-reviewer` agent after implementation |
| Writing or reviewing DB migrations | `db-analyst` agent or `/database-design` skill |
| Before merging any PR | `security-reviewer` agent |
| Encountering a bug or test failure | `/superpowers:systematic-debugging` skill |
| Starting implementation of a multi-step feature | `/superpowers:writing-plans` skill |

## Document References

Read the following documents based on the type of task before starting work.

**Planning a new feature or creating an implementation plan**
- MUST read [docs/product-specs/index.md](./docs/product-specs/index.md) — check current feature status before planning
- MUST read the relevant spec file under `docs/product-specs/` if one exists for the feature
- Read [docs/generated/db-schema.md](./docs/generated/db-schema.md) if the feature involves database changes

**Working on API layer**
- Read the relevant spec in `docs/product-specs/`
- See [api/AGENTS.md](./api/AGENTS.md) for conventions

**Working on batch jobs**
- See [batch/AGENTS.md](./batch/AGENTS.md) for job structure conventions

**Working on database migrations**
- MUST read [docs/generated/db-schema.md](./docs/generated/db-schema.md) to understand the current schema before writing a migration
- See [domain/AGENTS.md](./domain/AGENTS.md) for migration naming rules
