# domain/AGENTS.md

Business logic, JPA entities, repositories, Flyway migrations, QueryDSL.

## Test Commands

```bash
./gradlew :domain:test
./gradlew :domain:test --tests "com.techinsights.service.CompanyServiceTest"
```

Tests use **Kotest `FunSpec`** (lambda body style) + **Mockk**. Repositories are mocked — no DB required.

## Repository Pattern

Each aggregate has three layers:

1. **Interface** (`PostRepository`) — contract used by services
2. **JpaRepository** (`PostJpaRepository`) — Spring Data, simple CRUD
3. **Impl** (`PostRepositoryImpl`) — QueryDSL complex queries, delegates simple ops to JpaRepository

New queries: add to the interface → implement in `Impl` using `JPAQueryFactory`.

If Q-types are missing after adding a new entity:
```bash
./gradlew :domain:kaptKotlin
```

## Entity Conventions

- IDs: use `Tsid.generate()` — **not** `@GeneratedValue`
- Extend `BaseEntity` for `createdAt` / `updatedAt`

## Flyway Migrations

Path: `domain/src/main/resources/db/migration/`

- Format: `V{NNN}__{Description}.sql` — 3-digit zero-padded (e.g. `V016__Add_Column.sql`)
- Never modify existing migration files
