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
- `.faultTolerant()` with skip exceptions on the step

## Job Execution

Triggered by GitHub Actions (`.github/workflows/batch-*.yml`) — not scheduled within the app.

## Packages

| Package | Purpose |
|---------|---------|
| `crawling` | Crawls tech blog posts |
| `embedding` | pgvector embeddings via Gemini |
| `summary` | Post summarization via Gemini |
| `github` | GitHub trending repos |
| `infrastructure` | DB / external client config |
| `support` | Test mocks and utilities |
| `common` | Shared listeners |
