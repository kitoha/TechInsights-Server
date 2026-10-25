# batch/AGENTS.md

Spring Batch jobs for crawling, embedding, summarization, GitHub trending. Depends on `domain`.

## Test Commands

```bash
./gradlew :batch:test
./gradlew :batch:test --tests "com.techinsights.batch.embedding.reader.BatchSummarizedPostReaderTest"
```

Tests use **Kotest `FunSpec`** (lambda body style) + **Mockk**. No Spring context.

## Job Structure

```
{domain}/
  config/     ← Job + Step bean definitions
  reader/     ← ItemReader<I>
  processor/  ← ItemProcessor<I, O>
  writer/     ← ItemWriter<O>
  dto/        ← job-local DTOs
```

Every job config must include:
- `LoggingJobExecutionListener` on the job
- `.faultTolerant()` with skip exceptions on the step — **chunk-oriented steps only**

**Exception for tasklet steps** (e.g. `CompanyPostCountReconcileTasklet`, `RefreshTokenCleanupTasklet`): the fault-tolerance
API (`.faultTolerant()`, skip/retry) exists only on chunk-oriented step builders, so a tasklet step cannot use it. A
tasklet is one atomic, idempotent unit of work: a failure fails the step and the job (visible through
`LoggingJobExecutionListener` and the workflow's failure notification) and the job is simply re-run. The closest
equivalent is therefore the `LoggingJobExecutionListener` plus keeping the tasklet idempotent — a tasklet must not
swallow exceptions to "skip" them.

The `RefreshTokenCleanupTasklet` derives its retired-hash retention from `auth.jwt.refresh-token-expiration` (the api's
refresh-token lifetime, default `30d`); run the batch with the same value as the api.

## Job Execution

Triggered by GitHub Actions (`.github/workflows/batch-*.yml`) — not scheduled within the app.

## Packages

| Package | Purpose |
|---------|---------|
| `crawling` | Crawls tech blog posts |
| `embedding` | pgvector embeddings via Gemini |
| `summary` | Post summarization via Gemini |
| `github` | GitHub trending repos |
| `maintenance` | DB cleanup tasklets (e.g. expired refresh tokens) |
| `infrastructure` | DB / external client config |
| `support` | Test mocks and utilities |
| `common` | Shared listeners |
