# Batch 시스템 현황 분석 및 데이터 수집 가이드

## 목차
1. [Spring Batch 메타데이터 쿼리](#1-spring-batch-메타데이터-쿼리)
2. [성능 측정 도구 추가](#2-성능-측정-도구-추가)
3. [로그 분석 스크립트](#3-로그-분석-스크립트)
4. [데이터 수집 절차](#4-데이터-수집-절차)
5. [기대 메트릭 및 KPI](#5-기대-메트릭-및-kpi)

---

## 1. Spring Batch 메타데이터 쿼리

Spring Batch는 실행 정보를 자동으로 PostgreSQL에 저장합니다. 다음 쿼리로 현재 상태를 분석할 수 있습니다.

### 1.1 최근 배치 실행 이력 조회

```sql
-- 최근 30일간 배치 Job 실행 통계
SELECT
    ji.job_name,
    COUNT(*) as total_executions,
    COUNT(CASE WHEN je.status = 'COMPLETED' THEN 1 END) as successful,
    COUNT(CASE WHEN je.status = 'FAILED' THEN 1 END) as failed,
    ROUND(AVG(EXTRACT(EPOCH FROM (je.end_time - je.start_time))), 2) as avg_duration_seconds,
    MAX(EXTRACT(EPOCH FROM (je.end_time - je.start_time))) as max_duration_seconds,
    MIN(EXTRACT(EPOCH FROM (je.end_time - je.start_time))) as min_duration_seconds
FROM batch_job_execution je
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
WHERE je.create_time >= NOW() - INTERVAL '30 days'
GROUP BY ji.job_name
ORDER BY ji.job_name;
```

**예상 출력:**
```
job_name              | total | success | failed | avg_sec | max_sec | min_sec
---------------------|-------|---------|--------|---------|---------|--------
crawlPostJob         |    30 |      25 |      5 |  450.23 | 1200.50 |  180.30
summarizePostJob     |    28 |      20 |      8 |  3600.5 | 7200.00 | 1800.00
summaryAndEmbedding  |    15 |      10 |      5 |  5400.2 | 9000.00 | 3000.00
```

### 1.2 Step 별 성능 분석

```sql
-- Step별 처리량 및 Skip/Failure 통계
SELECT
    ji.job_name,
    se.step_name,
    COUNT(*) as executions,
    ROUND(AVG(se.read_count), 2) as avg_read,
    ROUND(AVG(se.write_count), 2) as avg_write,
    SUM(se.skip_count) as total_skipped,
    SUM(se.rollback_count) as total_rollbacks,
    ROUND(AVG(EXTRACT(EPOCH FROM (se.end_time - se.start_time))), 2) as avg_step_duration_sec
FROM batch_step_execution se
JOIN batch_job_execution je ON se.job_execution_id = je.job_execution_id
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
WHERE se.start_time >= NOW() - INTERVAL '30 days'
GROUP BY ji.job_name, se.step_name
ORDER BY ji.job_name, se.step_name;
```

**예상 출력:**
```
job_name         | step_name              | avg_read | avg_write | total_skip | avg_sec
----------------|------------------------|----------|-----------|------------|--------
crawlPostJob    | crawlPostStep          |    13.00 |      8.50 |         45 |  420.00
summarizePost   | summarizePostStep      |   120.50 |    110.20 |        105 | 3200.00
```

### 1.3 실패 패턴 분석

```sql
-- 최근 실패한 Job의 상세 정보
SELECT
    ji.job_name,
    je.job_execution_id,
    je.start_time,
    je.end_time,
    EXTRACT(EPOCH FROM (je.end_time - je.start_time)) as duration_seconds,
    je.status,
    je.exit_code,
    je.exit_message,
    se.step_name,
    se.read_count,
    se.write_count,
    se.skip_count,
    se.rollback_count
FROM batch_job_execution je
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
LEFT JOIN batch_step_execution se ON je.job_execution_id = se.job_execution_id
WHERE je.status != 'COMPLETED'
  AND je.start_time >= NOW() - INTERVAL '7 days'
ORDER BY je.start_time DESC
LIMIT 50;
```

### 1.4 처리량 추이 분석

```sql
-- 일별 처리량 추이 (최근 30일)
SELECT
    DATE(je.start_time) as execution_date,
    ji.job_name,
    COUNT(*) as executions,
    SUM(se.write_count) as total_items_processed,
    ROUND(AVG(EXTRACT(EPOCH FROM (je.end_time - je.start_time))), 2) as avg_duration_sec
FROM batch_job_execution je
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
LEFT JOIN batch_step_execution se ON je.job_execution_id = se.job_execution_id
WHERE je.start_time >= NOW() - INTERVAL '30 days'
  AND je.status = 'COMPLETED'
GROUP BY DATE(je.start_time), ji.job_name
ORDER BY execution_date DESC, ji.job_name;
```

---

## 2. 성능 측정 도구 추가

### 2.1 Enhanced Logging Listener (임시 성능 측정용)

**파일 위치:** `batch/src/main/kotlin/com/techinsights/batch/listener/BaselineMetricsListener.kt`

```kotlin
package com.techinsights.batch.listener

import org.slf4j.LoggerFactory
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime

/**
 * 개선 전 Baseline 성능 측정을 위한 임시 Listener
 *
 * 수집 데이터:
 * - 전체 Job 소요 시간
 * - Step별 소요 시간
 * - 처리량 (items/sec)
 * - 메모리 사용량
 */
@Component
class BaselineMetricsListener : JobExecutionListener, StepExecutionListener {

    private val log = LoggerFactory.getLogger(BaselineMetricsListener::class.java)

    private data class StepMetrics(
        val stepName: String,
        val startTime: LocalDateTime,
        var endTime: LocalDateTime? = null,
        var readCount: Long = 0,
        var writeCount: Long = 0,
        var skipCount: Long = 0
    )

    private val stepMetricsMap = mutableMapOf<Long, StepMetrics>()

    override fun beforeJob(jobExecution: JobExecution) {
        log.info("""
            ========================================
            📊 BASELINE METRICS - Job Starting
            ========================================
            Job Name: ${jobExecution.jobInstance.jobName}
            Job ID: ${jobExecution.jobExecutionId}
            Start Time: ${jobExecution.startTime}
            Parameters: ${jobExecution.jobParameters}

            System Info:
            - Available Processors: ${Runtime.getRuntime().availableProcessors()}
            - Max Memory: ${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB
            - Free Memory: ${Runtime.getRuntime().freeMemory() / 1024 / 1024} MB
            ========================================
        """.trimIndent())
    }

    override fun afterJob(jobExecution: JobExecution) {
        val duration = Duration.between(jobExecution.startTime, jobExecution.endTime)
        val totalSeconds = duration.seconds

        val totalRead = jobExecution.stepExecutions.sumOf { it.readCount }
        val totalWrite = jobExecution.stepExecutions.sumOf { it.writeCount }
        val totalSkip = jobExecution.stepExecutions.sumOf { it.skipCount }

        val throughput = if (totalSeconds > 0) totalWrite.toDouble() / totalSeconds else 0.0

        log.info("""
            ========================================
            📊 BASELINE METRICS - Job Completed
            ========================================
            Job Name: ${jobExecution.jobInstance.jobName}
            Status: ${jobExecution.status}
            Exit Code: ${jobExecution.exitStatus.exitCode}

            ⏱️  Duration:
            - Total: ${formatDuration(duration)}
            - Start: ${jobExecution.startTime}
            - End: ${jobExecution.endTime}

            📈 Processing Stats:
            - Items Read: $totalRead
            - Items Written: $totalWrite
            - Items Skipped: $totalSkip
            - Throughput: ${String.format("%.2f", throughput)} items/sec

            💾 Memory Usage:
            - Max Memory: ${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB
            - Total Memory: ${Runtime.getRuntime().totalMemory() / 1024 / 1024} MB
            - Free Memory: ${Runtime.getRuntime().freeMemory() / 1024 / 1024} MB
            - Used Memory: ${(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024} MB

            🔧 Step Breakdown:
            ${generateStepBreakdown(jobExecution)}

            ${if (jobExecution.allFailureExceptions.isNotEmpty()) {
                """
                ❌ Failures:
                ${jobExecution.allFailureExceptions.joinToString("\n") {
                    "- ${it.javaClass.simpleName}: ${it.message}"
                }}
                """.trimIndent()
            } else ""}
            ========================================
        """.trimIndent())

        // CSV 형식으로도 출력 (분석 용이)
        log.info("BASELINE_CSV,${jobExecution.jobInstance.jobName},${jobExecution.jobExecutionId}," +
                "${totalSeconds},${totalRead},${totalWrite},${totalSkip},${throughput}," +
                "${jobExecution.status},${jobExecution.exitStatus.exitCode}")
    }

    override fun beforeStep(stepExecution: StepExecution): Unit {
        val metrics = StepMetrics(
            stepName = stepExecution.stepName,
            startTime = stepExecution.startTime
        )
        stepMetricsMap[stepExecution.id] = metrics

        log.info("🔹 Step [${stepExecution.stepName}] starting at ${stepExecution.startTime}")
    }

    override fun afterStep(stepExecution: StepExecution): org.springframework.batch.core.ExitStatus {
        val metrics = stepMetricsMap[stepExecution.id]
        metrics?.endTime = stepExecution.endTime
        metrics?.readCount = stepExecution.readCount
        metrics?.writeCount = stepExecution.writeCount
        metrics?.skipCount = stepExecution.skipCount

        val duration = Duration.between(stepExecution.startTime, stepExecution.endTime)
        val seconds = duration.seconds
        val throughput = if (seconds > 0) stepExecution.writeCount.toDouble() / seconds else 0.0

        log.info("""
            🔹 Step [${stepExecution.stepName}] completed
            - Duration: ${formatDuration(duration)}
            - Read: ${stepExecution.readCount}
            - Write: ${stepExecution.writeCount}
            - Skip: ${stepExecution.skipCount}
            - Rollback: ${stepExecution.rollbackCount}
            - Throughput: ${String.format("%.2f", throughput)} items/sec
        """.trimIndent())

        return stepExecution.exitStatus
    }

    private fun generateStepBreakdown(jobExecution: JobExecution): String {
        return jobExecution.stepExecutions.joinToString("\n") { step ->
            val duration = Duration.between(step.startTime, step.endTime)
            val seconds = duration.seconds
            val throughput = if (seconds > 0) step.writeCount.toDouble() / seconds else 0.0

            """
            │ ${step.stepName}
            │   Duration: ${formatDuration(duration)}
            │   Read: ${step.readCount} | Write: ${step.writeCount} | Skip: ${step.skipCount}
            │   Throughput: ${String.format("%.2f", throughput)} items/sec
            """.trimIndent()
        }
    }

    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}
```

### 2.2 Company별 처리 시간 추적 Processor

**파일 위치:** `batch/src/main/kotlin/com/techinsights/batch/processor/MetricsTrackingRawPostProcessor.kt`

```kotlin
package com.techinsights.batch.processor

import com.techinsights.batch.crawling.PostCrawlingService
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import kotlinx.coroutines.runBlocking
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Baseline 측정용 - 기존 RawPostProcessor를 확장하여 회사별 처리 시간 측정
 */
@Component("metricsTrackingRawPostProcessor")
class MetricsTrackingRawPostProcessor(
    private val postCrawlingService: PostCrawlingService
) : ItemProcessor<CompanyDto, List<PostDto>> {

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(MetricsTrackingRawPostProcessor::class.java)

        // 전역 메트릭 수집 (Job 실행 간 공유)
        private val companyProcessingTimes = ConcurrentHashMap<String, MutableList<Long>>()
        private val currentIndex = AtomicInteger(0)
        private var totalCompanies = 0
    }

    override fun process(company: CompanyDto): List<PostDto> {
        if (totalCompanies == 0) {
            // 첫 실행 시 초기화 (실제로는 CompanyReader에서 total count를 가져와야 함)
            totalCompanies = 13  // 현재 회사 수
            currentIndex.set(0)
        }

        val current = currentIndex.incrementAndGet()
        val progress = (current.toDouble() / totalCompanies * 100).toInt()

        log.info("🔄 [$current/$totalCompanies] ($progress%) Processing: ${company.name}")

        val startTime = System.currentTimeMillis()

        return runBlocking {
            try {
                val result = postCrawlingService.processCrawledData(company)
                val duration = System.currentTimeMillis() - startTime

                // 처리 시간 기록
                companyProcessingTimes
                    .computeIfAbsent(company.name) { mutableListOf() }
                    .add(duration)

                log.info("✅ [$current/$totalCompanies] ${company.name}: " +
                        "${result.size} posts in ${duration}ms (${duration/1000}s)")

                // CSV 형식 로그 (분석용)
                log.info("COMPANY_METRIC,${company.name},${result.size},$duration")

                result
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                log.error("❌ [$current/$totalCompanies] ${company.name} FAILED after ${duration}ms: ${e.message}")

                log.info("COMPANY_METRIC,${company.name},0,$duration,FAILED,${e.javaClass.simpleName}")

                throw e
            }
        }
    }

    // Job 종료 후 호출하여 통계 출력
    fun printStatistics() {
        log.info("""
            ========================================
            📊 Company Processing Statistics
            ========================================
            ${companyProcessingTimes.entries.sortedByDescending {
                it.value.average()
            }.joinToString("\n") { (company, times) ->
                val avg = times.average()
                val min = times.minOrNull() ?: 0
                val max = times.maxOrNull() ?: 0
                "$company: avg=${avg.toLong()}ms, min=${min}ms, max=${max}ms, executions=${times.size}"
            }}
            ========================================
        """.trimIndent())
    }
}
```

### 2.3 Listener 설정에 추가

**파일 위치:** `batch/src/main/kotlin/com/techinsights/batch/config/PostCrawlingBatchConfig.kt`

기존 파일에 새로운 Listener 추가:

```kotlin
@Configuration
class PostCrawlingBatchConfig (
  private val jobRepository: JobRepository,
  private val transactionManager: PlatformTransactionManager,
  private val companyReader: CompanyReader,
  private val rawPostProcessor: RawPostProcessor,
  private val rawPostWriter: RawPostWriter,
  private val properties: PostCrawlingBatchProperties,
  private val loggingJobExecutionListener: LoggingJobExecutionListener,
  private val baselineMetricsListener: BaselineMetricsListener  // 추가
){

  @Bean
  fun crawlPostJob(@Qualifier("crawlPostStep") crawlPostStep: Step): Job =
    JobBuilder(properties.jobName, jobRepository)
      .incrementer(RunIdIncrementer())
      .listener(loggingJobExecutionListener)
      .listener(baselineMetricsListener)  // 추가
      .start(crawlPostStep)
      .build()

  // ... 나머지 코드
}
```

---

## 3. 로그 분석 스크립트

### 3.1 로그 파싱 스크립트 (Python)

**파일 위치:** `scripts/analyze_batch_logs.py`

```python
#!/usr/bin/env python3
"""
Batch 로그 파일을 분석하여 Baseline 메트릭을 추출하는 스크립트

사용법:
  python scripts/analyze_batch_logs.py /path/to/batch.log

출력:
  - 배치 실행 통계
  - 회사별 처리 시간
  - 실패 분석
  - CSV 리포트
"""

import re
import sys
from collections import defaultdict
from datetime import datetime
from pathlib import Path
import statistics

class BatchLogAnalyzer:
    def __init__(self, log_file):
        self.log_file = log_file
        self.job_executions = []
        self.company_metrics = defaultdict(list)
        self.failures = []

    def parse(self):
        """로그 파일 파싱"""
        with open(self.log_file, 'r', encoding='utf-8') as f:
            current_job = None

            for line in f:
                # Job 시작 감지
                if 'BASELINE METRICS - Job Starting' in line:
                    current_job = {'start_line': line}

                # Job 종료 감지
                elif 'BASELINE METRICS - Job Completed' in line and current_job:
                    current_job['end_line'] = line
                    self.parse_job_metrics(current_job)
                    current_job = None

                # Company 메트릭 파싱
                elif 'COMPANY_METRIC' in line:
                    self.parse_company_metric(line)

                # CSV 형식 메트릭 파싱
                elif 'BASELINE_CSV' in line:
                    self.parse_csv_metric(line)

                # 실패 로그 감지
                elif 'FAILED' in line or 'ERROR' in line:
                    self.failures.append(line)

    def parse_job_metrics(self, job_data):
        """Job 실행 메트릭 추출"""
        # 정규식으로 메트릭 추출
        # 실제 구현은 로그 형식에 맞게 조정 필요
        pass

    def parse_company_metric(self, line):
        """회사별 메트릭 추출

        형식: COMPANY_METRIC,{company},{posts},{duration_ms}[,FAILED,{error}]
        """
        parts = line.split('COMPANY_METRIC,')[1].strip().split(',')
        if len(parts) >= 3:
            company = parts[0]
            posts = int(parts[1])
            duration = int(parts[2])
            status = 'SUCCESS' if len(parts) < 4 else parts[3]

            self.company_metrics[company].append({
                'posts': posts,
                'duration_ms': duration,
                'status': status
            })

    def parse_csv_metric(self, line):
        """CSV 형식 메트릭 파싱

        형식: BASELINE_CSV,{job_name},{job_id},{duration},{read},{write},{skip},{throughput},{status},{exit_code}
        """
        parts = line.split('BASELINE_CSV,')[1].strip().split(',')
        if len(parts) >= 9:
            self.job_executions.append({
                'job_name': parts[0],
                'job_id': parts[1],
                'duration_sec': int(parts[2]),
                'read': int(parts[3]),
                'write': int(parts[4]),
                'skip': int(parts[5]),
                'throughput': float(parts[6]),
                'status': parts[7],
                'exit_code': parts[8]
            })

    def generate_report(self):
        """리포트 생성"""
        print("=" * 80)
        print("📊 BATCH BASELINE ANALYSIS REPORT")
        print("=" * 80)
        print()

        # Job 실행 통계
        if self.job_executions:
            print("📈 Job Execution Summary")
            print("-" * 80)
            for job in self.job_executions:
                print(f"Job: {job['job_name']} (ID: {job['job_id']})")
                print(f"  Duration: {job['duration_sec']}s ({job['duration_sec']//60}m {job['duration_sec']%60}s)")
                print(f"  Processed: {job['write']}/{job['read']} items")
                print(f"  Skipped: {job['skip']} items")
                print(f"  Throughput: {job['throughput']:.2f} items/sec")
                print(f"  Status: {job['status']}")
                print()

        # 회사별 통계
        if self.company_metrics:
            print("🏢 Company Processing Statistics")
            print("-" * 80)

            stats = []
            for company, metrics in self.company_metrics.items():
                durations = [m['duration_ms'] for m in metrics]
                posts = [m['posts'] for m in metrics]
                successes = sum(1 for m in metrics if m['status'] == 'SUCCESS')

                stats.append({
                    'company': company,
                    'avg_duration': statistics.mean(durations) if durations else 0,
                    'min_duration': min(durations) if durations else 0,
                    'max_duration': max(durations) if durations else 0,
                    'avg_posts': statistics.mean(posts) if posts else 0,
                    'executions': len(metrics),
                    'success_rate': (successes / len(metrics) * 100) if metrics else 0
                })

            # 평균 처리 시간 기준 정렬
            stats.sort(key=lambda x: x['avg_duration'], reverse=True)

            print(f"{'Company':<20} {'Avg Time':<12} {'Min':<10} {'Max':<10} {'Avg Posts':<10} {'Success %':<10}")
            print("-" * 80)
            for s in stats:
                print(f"{s['company']:<20} "
                      f"{s['avg_duration']/1000:>10.1f}s "
                      f"{s['min_duration']/1000:>9.1f}s "
                      f"{s['max_duration']/1000:>9.1f}s "
                      f"{s['avg_posts']:>9.1f} "
                      f"{s['success_rate']:>9.1f}%")
            print()

        # 실패 분석
        if self.failures:
            print("❌ Failure Analysis")
            print("-" * 80)
            print(f"Total failures detected: {len(self.failures)}")

            # 실패 유형 분류
            error_types = defaultdict(int)
            for failure in self.failures:
                if 'TimeoutException' in failure:
                    error_types['Timeout'] += 1
                elif 'ConnectException' in failure:
                    error_types['Connection'] += 1
                elif 'ParseException' in failure or 'parsing' in failure.lower():
                    error_types['Parse Error'] += 1
                else:
                    error_types['Other'] += 1

            print("\nError Types:")
            for error_type, count in sorted(error_types.items(), key=lambda x: x[1], reverse=True):
                print(f"  {error_type}: {count}")
            print()

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: python analyze_batch_logs.py <log_file>")
        sys.exit(1)

    log_file = sys.argv[1]
    if not Path(log_file).exists():
        print(f"Error: File not found: {log_file}")
        sys.exit(1)

    analyzer = BatchLogAnalyzer(log_file)
    analyzer.parse()
    analyzer.generate_report()
```

### 3.2 로그 추출 쉘 스크립트

**파일 위치:** `scripts/extract_batch_metrics.sh`

```bash
#!/bin/bash
# Batch 로그에서 메트릭만 추출하는 스크립트

LOG_FILE=${1:-/var/log/batch/batch.log}
OUTPUT_DIR=${2:-./batch-metrics}

mkdir -p "$OUTPUT_DIR"

echo "Extracting metrics from $LOG_FILE to $OUTPUT_DIR"

# Job 실행 통계 추출
echo "Extracting job executions..."
grep "BASELINE_CSV" "$LOG_FILE" > "$OUTPUT_DIR/job_executions.csv"

# 회사별 메트릭 추출
echo "Extracting company metrics..."
grep "COMPANY_METRIC" "$LOG_FILE" > "$OUTPUT_DIR/company_metrics.csv"

# 에러 로그 추출
echo "Extracting errors..."
grep -E "ERROR|FAILED|Exception" "$LOG_FILE" > "$OUTPUT_DIR/errors.log"

# Step 통계 추출
echo "Extracting step statistics..."
grep -A 5 "Step \[.*\] completed" "$LOG_FILE" > "$OUTPUT_DIR/step_statistics.log"

echo "Done! Metrics extracted to $OUTPUT_DIR"
ls -lh "$OUTPUT_DIR"
```

---

## 4. 데이터 수집 절차

### Phase 1: 사전 준비 (1일)

1. **코드 배포**
   ```bash
   # BaselineMetricsListener 추가
   cd batch/src/main/kotlin/com/techinsights/batch/listener
   # 위의 BaselineMetricsListener.kt 파일 생성

   # 빌드 및 배포
   ./gradlew :batch:build
   # 서버에 배포
   ```

2. **로그 레벨 설정**
   ```yaml
   # application.yml에 추가
   logging:
     level:
       com.techinsights.batch: INFO
       org.springframework.batch: INFO
     file:
       name: /var/log/batch/batch.log
       max-size: 100MB
       max-history: 30
   ```

### Phase 2: 데이터 수집 (7-14일)

1. **일일 배치 실행 및 모니터링**
   ```bash
   # 배치 실행
   java -jar batch.jar --spring.batch.job.names=crawlPostJob

   # 로그 확인
   tail -f /var/log/batch/batch.log | grep "BASELINE"
   ```

2. **일일 메트릭 추출**
   ```bash
   # 매일 실행
   ./scripts/extract_batch_metrics.sh /var/log/batch/batch.log ./metrics/$(date +%Y%m%d)
   ```

3. **데이터베이스 스냅샷**
   ```bash
   # 매일 배치 실행 후
   psql -h localhost -U user -d techinsights -f scripts/export_batch_metadata.sql > ./metrics/$(date +%Y%m%d)/batch_metadata.csv
   ```

### Phase 3: 분석 및 리포트 (2-3일)

1. **로그 분석**
   ```bash
   python scripts/analyze_batch_logs.py /var/log/batch/batch.log > baseline_report.txt
   ```

2. **데이터베이스 쿼리 실행**
   - 위의 SQL 쿼리들을 모두 실행하여 결과 저장

3. **Excel/CSV 정리**
   - 수집된 데이터를 스프레드시트로 정리
   - 그래프 생성 (처리 시간 추이, 실패율 등)

---

## 5. 기대 메트릭 및 KPI

### 5.1 수집할 핵심 메트릭

| 카테고리 | 메트릭 | 목표 값 | 측정 방법 |
|---------|--------|---------|----------|
| **성능** |
| | 전체 배치 소요 시간 | < 30분 | Job Execution Duration |
| | 회사당 평균 처리 시간 | < 2분 | Company Metrics 평균 |
| | 가장 느린 회사 처리 시간 | < 5분 | Company Metrics 최댓값 |
| | 처리량 (throughput) | > 100 posts/min | Total Write / Duration |
| **안정성** |
| | 배치 성공률 | > 95% | Successful Jobs / Total Jobs |
| | 회사별 성공률 | > 90% | Per-company Success Rate |
| | Skip 비율 | < 5% | Skip Count / Read Count |
| **병렬성** |
| | 동시 처리 회사 수 | 1 (순차) | 현재 아키텍처 제약 |
| | CPU 사용률 | < 30% | 시스템 모니터링 |
| **데이터 품질** |
| | 중복 방지율 | 100% | 중복 URL 검출 |
| | 데이터 신선도 | < 24시간 | Last Crawl Time |

### 5.2 분석할 질문들

1. **외부 의존성 지연 (조건 1)**
   - Q: 가장 느린 회사는 어디이며, 얼마나 느린가?
   - Q: 특정 회사의 타임아웃이 전체 배치 시간에 미치는 영향은?
   - 측정: `max(company_duration) / avg(company_duration)` 비율

2. **실패 재실행 (조건 2)**
   - Q: 지난 30일간 실패한 회사는 총 몇 개인가?
   - Q: 동일 회사가 반복적으로 실패하는가?
   - 측정: Failure 테이블 쿼리

3. **SLA (조건 3)**
   - Q: 현재 평균 배치 완료 시간은?
   - Q: 최악의 경우 배치 완료 시간은?
   - Q: SLA를 30분으로 설정했을 때 준수율은?
   - 측정: Job Duration 통계

4. **모니터링 (조건 4)**
   - Q: 현재 배치가 어느 회사를 처리 중인지 알 수 있는가?
   - Q: 실패한 회사 목록을 즉시 확인할 수 있는가?
   - 측정: 로그 분석

5. **장애 추적 (조건 5)**
   - Q: 실패 유형별 분포는?
   - Q: 가장 흔한 실패 원인은?
   - 측정: Exception Type 분류

6. **안전한 재실행 (조건 6)**
   - Q: Skip Limit 10개는 적절한가?
   - Q: 전체 회사의 몇 %가 실패해도 괜찮은가?
   - 측정: Skip Count 통계

7. **데이터 신선도 (조건 7)**
   - Q: 가장 오래된 데이터는 언제 수집되었는가?
   - Q: 24시간 이내 데이터 비율은?
   - 측정: Post.publishedAt 분석

8. **Idempotency (조건 8)**
   - Q: 중복 URL이 저장되는 경우가 있는가?
   - Q: 재실행 시 Gemini API를 중복 호출하는가?
   - 측정: 로그 분석, API 호출 횟수

---

## 6. 리포트 템플릿

### 6.1 주간 Baseline 리포트

```markdown
# Batch Baseline Report - Week of YYYY-MM-DD

## Executive Summary
- Total Batch Runs: XX
- Success Rate: XX%
- Average Duration: XXm XXs
- Total Items Processed: XXXX

## Performance Metrics

### Job-Level Performance
| Metric | Value | Target | Status |
|--------|-------|--------|--------|
| Avg Duration | XXm | 30m | 🔴/🟡/🟢 |
| Max Duration | XXm | 45m | 🔴/🟡/🟢 |
| Throughput | XX items/sec | 10 items/sec | 🔴/🟡/🟢 |

### Company-Level Performance
| Company | Avg Time | Max Time | Success Rate | Posts/Run |
|---------|----------|----------|--------------|-----------|
| Company A | Xs | Xs | XX% | XX |
| ... |

## Failure Analysis

### Failure Rate by Type
| Type | Count | Percentage |
|------|-------|------------|
| Timeout | XX | XX% |
| Connection | XX | XX% |
| Parse Error | XX | XX% |

### Top Failing Companies
1. Company A - XX failures (Reason: ...)
2. Company B - XX failures (Reason: ...)

## Recommendations

### High Priority
1. [Issue]: 특정 회사 타임아웃 빈번
   - Impact: 전체 배치 시간 XX% 증가
   - Recommendation: 병렬 처리 도입

### Medium Priority
...

## Appendix
- Raw Data: [링크]
- SQL Queries: [링크]
- Log Files: [링크]
```

---

## 7. 데이터 시각화

### 7.1 Grafana 대시보드 (선택사항)

Spring Boot Actuator + Micrometer를 사용하여 메트릭을 Prometheus로 export하고 Grafana에서 시각화

**추가 dependency:**
```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
}
```

**application.yml:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 7.2 간단한 HTML 리포트 생성

**파일 위치:** `scripts/generate_html_report.py`

```python
#!/usr/bin/env python3
"""
수집된 메트릭을 HTML 리포트로 생성
"""

import json
from datetime import datetime

def generate_html_report(metrics_data, output_file='baseline_report.html'):
    html = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <title>Batch Baseline Report</title>
        <style>
            body {{ font-family: Arial, sans-serif; margin: 20px; }}
            table {{ border-collapse: collapse; width: 100%; margin: 20px 0; }}
            th, td {{ border: 1px solid #ddd; padding: 12px; text-align: left; }}
            th {{ background-color: #4CAF50; color: white; }}
            .metric-good {{ color: green; font-weight: bold; }}
            .metric-warning {{ color: orange; font-weight: bold; }}
            .metric-bad {{ color: red; font-weight: bold; }}
            h1 {{ color: #333; }}
            h2 {{ color: #666; border-bottom: 2px solid #4CAF50; }}
        </style>
    </head>
    <body>
        <h1>📊 Batch System Baseline Report</h1>
        <p>Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>

        <h2>Job Performance Summary</h2>
        <table>
            <tr>
                <th>Metric</th>
                <th>Current Value</th>
                <th>Target</th>
                <th>Status</th>
            </tr>
            <!-- 데이터를 채워넣기 -->
        </table>

        <h2>Company Processing Times</h2>
        <table>
            <tr>
                <th>Company</th>
                <th>Avg Duration</th>
                <th>Success Rate</th>
            </tr>
            <!-- 데이터를 채워넣기 -->
        </table>
    </body>
    </html>
    """

    with open(output_file, 'w') as f:
        f.write(html)

    print(f"HTML report generated: {output_file}")

if __name__ == '__main__':
    # 수집된 데이터를 로드하여 HTML 생성
    generate_html_report({})
```

---

## 8. 체크리스트

### 데이터 수집 준비
- [ ] BaselineMetricsListener 코드 추가
- [ ] 로그 레벨 및 파일 설정
- [ ] 스크립트 실행 권한 부여 (`chmod +x scripts/*.sh`)
- [ ] 로그 저장 디렉토리 생성

### 수집 기간 (7-14일)
- [ ] 매일 배치 실행
- [ ] 매일 로그 백업
- [ ] 매일 메트릭 추출
- [ ] 주간 중간 점검

### 분석
- [ ] SQL 쿼리 실행 및 결과 저장
- [ ] 로그 분석 스크립트 실행
- [ ] 데이터 시각화 (그래프)
- [ ] Baseline 리포트 작성

### 문서화
- [ ] 측정 방법 문서화
- [ ] 원시 데이터 보관
- [ ] 개선 전/후 비교를 위한 형식 통일

---

이제 이 가이드를 따라 데이터를 수집하면 개선 작업의 명확한 근거를 확보할 수 있습니다!
