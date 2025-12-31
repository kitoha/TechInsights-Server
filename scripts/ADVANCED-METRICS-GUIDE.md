# 빅테크 수준의 배치 성능 분석 가이드

## 🎯 목표: "느리다"가 아니라 "왜 느린가"를 데이터로 증명

---

## 📊 수집해야 할 핵심 메트릭

### 1. 컴퓨팅 효율성 (Resource Saturation)

**질문:** "CPU가 100% 돌아서 느린가? 아니면 네트워크 대기로 느린가?"

#### 측정 항목
- **CPU 사용률** (목표: 70-80%)
- **메모리 사용률** (목표: 70-80%)
- **네트워크 I/O** (외부 API 대기 시간)
- **디스크 I/O** (DB 쿼리 대기 시간)
- **스레드 상태** (RUNNABLE vs WAITING)

---

### 2. 데이터 스큐 (Data Skew)

**질문:** "특정 회사만 오래 걸려서 전체가 느린가?"

#### 측정 항목
- **회사별 처리 시간** (최소/최대/중간값/표준편차)
- **처리 시간 분포** (히스토그램)
- **Long Tail 분석** (상위 20% 회사가 전체 시간의 80% 차지하는가?)

---

### 3. 비용 효율성 (Cost per Record)

**질문:** "데이터 1건 처리에 얼마의 비용이 드는가?"

#### 측정 항목
- **인프라 비용 / 처리 건수** ($ per post)
- **Gemini API 비용 / 요약 건수**
- **EC2 시간당 비용 / 처리 속도**
- **월별 비용 추이**

---

## 🛠 측정 도구

### 도구 1: 리소스 모니터링 추가 (Spring Boot Actuator + Micrometer)

#### Step 1: Dependency 추가

```kotlin
// build.gradle.kts
dependencies {
    // 기존 dependencies...

    // Actuator (메트릭 수집)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Micrometer (메트릭 export)
    implementation("io.micrometer:micrometer-registry-prometheus")

    // JVM 메트릭
    runtimeOnly("io.micrometer:micrometer-core")
}
```

#### Step 2: application.yml 설정

```yaml
# batch/src/main/resources/application.yml

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: batch
      environment: production

  # JVM 메트릭 활성화
  metrics:
    enable:
      jvm: true
      process: true
      system: true
```

#### Step 3: 메트릭 수집 Listener

```kotlin
// batch/src/main/kotlin/com/techinsights/batch/listener/ResourceMetricsListener.kt

package com.techinsights.batch.listener

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.stereotype.Component
import java.lang.management.ManagementFactory
import java.time.Duration

@Component
class ResourceMetricsListener(
    private val meterRegistry: MeterRegistry
) : JobExecutionListener, StepExecutionListener {

    private val runtime = Runtime.getRuntime()
    private val osBean = ManagementFactory.getOperatingSystemMBean()
    private val threadBean = ManagementFactory.getThreadMXBean()

    private data class ResourceSnapshot(
        val timestamp: Long,
        val cpuLoad: Double,
        val memoryUsed: Long,
        val memoryFree: Long,
        val threadCount: Int,
        val waitingThreads: Int
    )

    private var jobStartSnapshot: ResourceSnapshot? = null
    private val stepSnapshots = mutableMapOf<Long, ResourceSnapshot>()

    override fun beforeJob(jobExecution: JobExecution) {
        jobStartSnapshot = captureResourceSnapshot()

        log.info("""
            ========================================
            📊 RESOURCE METRICS - Job Starting
            ========================================
            Job: ${jobExecution.jobInstance.jobName}

            Initial Resources:
            - CPU Cores: ${runtime.availableProcessors()}
            - Max Memory: ${runtime.maxMemory() / 1024 / 1024} MB
            - Used Memory: ${(runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024} MB
            - Free Memory: ${runtime.freeMemory() / 1024 / 1024} MB
            - Thread Count: ${threadBean.threadCount}
            - Daemon Threads: ${threadBean.daemonThreadCount}
            ========================================
        """.trimIndent())
    }

    override fun afterJob(jobExecution: JobExecution) {
        val endSnapshot = captureResourceSnapshot()
        val startSnapshot = jobStartSnapshot ?: return

        val duration = Duration.between(jobExecution.startTime, jobExecution.endTime)
        val durationSeconds = duration.seconds.toDouble()

        // 평균 리소스 사용량 계산
        val avgMemoryUsed = (startSnapshot.memoryUsed + endSnapshot.memoryUsed) / 2
        val avgCpuLoad = (startSnapshot.cpuLoad + endSnapshot.cpuLoad) / 2

        // Micrometer에 기록
        meterRegistry.gauge(
            "batch.job.cpu.utilization",
            listOf(
                io.micrometer.core.instrument.Tag.of("job", jobExecution.jobInstance.jobName),
                io.micrometer.core.instrument.Tag.of("status", jobExecution.status.name)
            ),
            avgCpuLoad * 100  // 퍼센트로 변환
        )

        meterRegistry.gauge(
            "batch.job.memory.used.mb",
            listOf(
                io.micrometer.core.instrument.Tag.of("job", jobExecution.jobInstance.jobName)
            ),
            avgMemoryUsed / 1024.0 / 1024.0
        )

        // 처리량 메트릭
        val totalProcessed = jobExecution.stepExecutions.sumOf { it.writeCount }
        val throughput = totalProcessed / durationSeconds

        meterRegistry.gauge(
            "batch.job.throughput.items_per_sec",
            listOf(
                io.micrometer.core.instrument.Tag.of("job", jobExecution.jobInstance.jobName)
            ),
            throughput
        )

        // 🔥 컴퓨팅 효율성 분석
        val cpuUtilization = avgCpuLoad * 100
        val efficiency = when {
            cpuUtilization < 20 -> "🔴 IDLE (I/O bound)"
            cpuUtilization < 50 -> "🟡 UNDER-UTILIZED"
            cpuUtilization < 80 -> "🟢 OPTIMAL"
            else -> "🔴 CPU SATURATED"
        }

        log.info("""
            ========================================
            📊 RESOURCE METRICS - Job Completed
            ========================================
            Job: ${jobExecution.jobInstance.jobName}
            Duration: ${durationSeconds}s

            🖥️  CPU Metrics:
            - Average CPU Load: ${String.format("%.2f", avgCpuLoad * 100)}%
            - Efficiency: $efficiency
            - Available Cores: ${runtime.availableProcessors()}

            💾 Memory Metrics:
            - Peak Memory Used: ${endSnapshot.memoryUsed / 1024 / 1024} MB
            - Average Memory: ${avgMemoryUsed / 1024 / 1024} MB
            - Max Available: ${runtime.maxMemory() / 1024 / 1024} MB
            - Memory Utilization: ${String.format("%.2f", (avgMemoryUsed.toDouble() / runtime.maxMemory()) * 100)}%

            🧵 Thread Metrics:
            - Peak Threads: ${endSnapshot.threadCount}
            - Waiting Threads: ${endSnapshot.waitingThreads}
            - Thread Utilization: ${String.format("%.2f", (endSnapshot.threadCount.toDouble() / runtime.availableProcessors()) * 100)}%

            📈 Processing Metrics:
            - Total Processed: $totalProcessed items
            - Throughput: ${String.format("%.2f", throughput)} items/sec
            - Cost per Item (CPU time): ${String.format("%.4f", durationSeconds / totalProcessed)}s

            ⚠️  Bottleneck Analysis:
            ${analyzeBottleneck(avgCpuLoad, endSnapshot)}
            ========================================
        """.trimIndent())

        // CSV 형식으로도 출력 (분석 용이)
        log.info("RESOURCE_METRICS_CSV," +
                "${jobExecution.jobInstance.jobName}," +
                "${jobExecution.jobExecutionId}," +
                "${durationSeconds}," +
                "${String.format("%.2f", avgCpuLoad * 100)}," +
                "${avgMemoryUsed / 1024 / 1024}," +
                "${endSnapshot.threadCount}," +
                "${endSnapshot.waitingThreads}," +
                "${totalProcessed}," +
                "${String.format("%.2f", throughput)}")
    }

    private fun captureResourceSnapshot(): ResourceSnapshot {
        // CPU 사용률 (0.0 ~ 1.0)
        val cpuLoad = when (osBean) {
            is com.sun.management.OperatingSystemMXBean ->
                osBean.processCpuLoad
            else -> -1.0
        }

        // 메모리
        val memoryUsed = runtime.totalMemory() - runtime.freeMemory()
        val memoryFree = runtime.freeMemory()

        // 스레드
        val threadCount = threadBean.threadCount
        val threadIds = threadBean.allThreadIds
        val waitingThreads = threadIds.count { id ->
            val info = threadBean.getThreadInfo(id)
            info?.threadState == Thread.State.WAITING ||
            info?.threadState == Thread.State.TIMED_WAITING
        }

        return ResourceSnapshot(
            timestamp = System.currentTimeMillis(),
            cpuLoad = cpuLoad,
            memoryUsed = memoryUsed,
            memoryFree = memoryFree,
            threadCount = threadCount,
            waitingThreads = waitingThreads
        )
    }

    private fun analyzeBottleneck(cpuLoad: Double, snapshot: ResourceSnapshot): String {
        val issues = mutableListOf<String>()

        // CPU 분석
        if (cpuLoad < 0.2) {
            issues.add("- 🔴 CPU Idle (${String.format("%.1f", cpuLoad * 100)}%) → I/O Bound (네트워크/DB 대기)")
        }

        // 메모리 분석
        val memoryUsage = snapshot.memoryUsed.toDouble() / runtime.maxMemory()
        if (memoryUsage > 0.8) {
            issues.add("- 🔴 High Memory Usage (${String.format("%.1f", memoryUsage * 100)}%) → GC 오버헤드")
        }

        // 스레드 분석
        val waitingRatio = snapshot.waitingThreads.toDouble() / snapshot.threadCount
        if (waitingRatio > 0.5) {
            issues.add("- 🔴 ${snapshot.waitingThreads}/${snapshot.threadCount} threads waiting → External I/O Bottleneck")
        }

        return if (issues.isEmpty()) {
            "✅ No obvious bottleneck detected"
        } else {
            issues.joinToString("\n")
        }
    }

    override fun beforeStep(stepExecution: StepExecution) {
        stepSnapshots[stepExecution.id] = captureResourceSnapshot()
    }

    override fun afterStep(stepExecution: StepExecution): org.springframework.batch.core.ExitStatus {
        // Step별 리소스 사용량도 기록 가능
        return stepExecution.exitStatus
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(ResourceMetricsListener::class.java)
    }
}
```

---

### 도구 2: 회사별 처리 시간 상세 측정 (Data Skew 분석)

```kotlin
// batch/src/main/kotlin/com/techinsights/batch/processor/SkewAnalysisProcessor.kt

package com.techinsights.batch.processor

import com.techinsights.batch.crawling.PostCrawlingService
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.runBlocking
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

@Component
class SkewAnalysisProcessor(
    private val postCrawlingService: PostCrawlingService,
    private val meterRegistry: MeterRegistry
) : ItemProcessor<CompanyDto, List<PostDto>> {

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(SkewAnalysisProcessor::class.java)

        // 회사별 처리 시간 (모든 실행 누적)
        private val companyDurations = ConcurrentHashMap<String, MutableList<Long>>()

        // 현재 실행의 회사별 시간
        private val currentRunDurations = ConcurrentHashMap<String, Long>()
    }

    override fun process(company: CompanyDto): List<PostDto> {
        val startTime = System.currentTimeMillis()

        return runBlocking {
            try {
                // Timer로 측정
                val timer = Timer.sample()

                val result = postCrawlingService.processCrawledData(company)

                val duration = System.currentTimeMillis() - startTime

                // 기록
                companyDurations
                    .computeIfAbsent(company.name) { mutableListOf() }
                    .add(duration)

                currentRunDurations[company.name] = duration

                // Micrometer에 기록
                timer.stop(meterRegistry.timer(
                    "batch.company.processing.time",
                    "company", company.name,
                    "status", "success"
                ))

                meterRegistry.counter(
                    "batch.company.posts.count",
                    "company", company.name
                ).increment(result.size.toDouble())

                log.info("✅ ${company.name}: ${result.size} posts in ${duration}ms (${duration/1000}s)")

                result
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime

                currentRunDurations[company.name] = duration

                meterRegistry.timer(
                    "batch.company.processing.time",
                    "company", company.name,
                    "status", "failed"
                ).record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)

                log.error("❌ ${company.name} FAILED after ${duration}ms: ${e.message}")
                throw e
            }
        }
    }

    // Job 종료 후 호출 (JobExecutionListener에서)
    fun analyzeSkew() {
        if (currentRunDurations.isEmpty()) return

        val durations = currentRunDurations.values.toList()
        val sorted = durations.sorted()

        // 통계 계산
        val min = sorted.minOrNull() ?: 0
        val max = sorted.maxOrNull() ?: 0
        val mean = durations.average()
        val median = sorted[sorted.size / 2]

        // 표준편차
        val variance = durations.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)

        // Percentiles
        val p50 = sorted[(sorted.size * 0.50).toInt()]
        val p95 = sorted[(sorted.size * 0.95).toInt()]
        val p99 = sorted[(sorted.size * 0.99).toInt()]

        // Long Tail 분석 (상위 20%가 전체의 몇 %를 차지?)
        val top20Percent = sorted.takeLast((sorted.size * 0.2).toInt())
        val top20Sum = top20Percent.sum()
        val totalSum = sorted.sum()
        val top20Contribution = (top20Sum.toDouble() / totalSum) * 100

        // Skewness 계산 (왜도)
        val skew = if (sorted.size > 2) {
            val n = sorted.size
            val m3 = durations.map { val d = it - mean; d * d * d }.sum() / n
            val s3 = stdDev * stdDev * stdDev
            m3 / s3
        } else 0.0

        log.info("""
            ========================================
            📊 DATA SKEW ANALYSIS
            ========================================
            Total Companies: ${durations.size}

            ⏱️  Processing Time Distribution:
            - Min: ${min}ms (${min/1000}s)
            - P50 (Median): ${median}ms (${median/1000}s)
            - Mean: ${String.format("%.2f", mean)}ms
            - P95: ${p95}ms (${p95/1000}s)
            - P99: ${p99}ms (${p99/1000}s)
            - Max: ${max}ms (${max/1000}s)
            - Std Dev: ${String.format("%.2f", stdDev)}ms

            🎯 Skewness Analysis:
            - Skewness: ${String.format("%.2f", skew)} ${interpretSkewness(skew)}
            - Max/Median Ratio: ${String.format("%.2f", max.toDouble() / median)}x
            - Top 20% Contribution: ${String.format("%.1f", top20Contribution)}%

            🔴 Slowest Companies (Top 5):
            ${getSlowestCompanies(5)}

            🟢 Fastest Companies (Top 5):
            ${getFastestCompanies(5)}

            ⚠️  Recommendation:
            ${generateRecommendation(skew, max.toDouble() / median, top20Contribution)}
            ========================================
        """.trimIndent())

        // CSV 출력
        currentRunDurations.forEach { (company, duration) ->
            log.info("SKEW_CSV,$company,$duration")
        }

        // 현재 실행 데이터 초기화
        currentRunDurations.clear()
    }

    private fun interpretSkewness(skew: Double): String = when {
        skew < -1 -> "🔴 Highly Left-Skewed (대부분 느림)"
        skew < -0.5 -> "🟡 Left-Skewed"
        skew > 1 -> "🔴 Highly Right-Skewed (소수만 매우 느림)"
        skew > 0.5 -> "🟡 Right-Skewed"
        else -> "🟢 Symmetric (균등 분포)"
    }

    private fun getSlowestCompanies(n: Int): String {
        return currentRunDurations.entries
            .sortedByDescending { it.value }
            .take(n)
            .mapIndexed { idx, (company, duration) ->
                "${idx + 1}. $company: ${duration}ms (${duration/1000}s)"
            }
            .joinToString("\n            ")
    }

    private fun getFastestCompanies(n: Int): String {
        return currentRunDurations.entries
            .sortedBy { it.value }
            .take(n)
            .mapIndexed { idx, (company, duration) ->
                "${idx + 1}. $company: ${duration}ms (${duration/1000}s)"
            }
            .joinToString("\n            ")
    }

    private fun generateRecommendation(
        skew: Double,
        maxMedianRatio: Double,
        top20Contribution: Double
    ): String {
        val issues = mutableListOf<String>()

        if (maxMedianRatio > 5) {
            issues.add("- 🔴 Max/Median ratio ${String.format("%.1f", maxMedianRatio)}x → 특정 회사가 전체 시간 지배")
        }

        if (top20Contribution > 60) {
            issues.add("- 🔴 상위 20% 회사가 전체의 ${String.format("%.1f", top20Contribution)}% 차지 → 심각한 Data Skew")
        }

        if (skew > 1) {
            issues.add("- 🟡 Right-Skewed 분포 → 소수 회사만 오래 걸림 → 병렬 처리 시 개선 효과 제한적")
        }

        return if (issues.isEmpty()) {
            "✅ 처리 시간이 균등하게 분포됨 → 병렬 처리 효과 극대화 가능"
        } else {
            issues.joinToString("\n            ")
        }
    }
}
```

---

### 도구 3: 비용 분석 (Cost per Record)

```kotlin
// batch/src/main/kotlin/com/techinsights/batch/listener/CostAnalysisListener.kt

package com.techinsights.batch.listener

import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CostAnalysisListener : JobExecutionListener {

    // AWS EC2 t2.micro 비용 (서울 리전, On-Demand, 2024 기준)
    private val ec2HourlyCost = 0.0136  // USD per hour

    // Gemini API 비용 (예시)
    // https://ai.google.dev/pricing
    private val geminiFlashSummaryCost = 0.000075  // per 1K characters input
    private val geminiEmbeddingCost = 0.00001      // per 1K characters

    override fun afterJob(jobExecution: JobExecution) {
        val duration = Duration.between(jobExecution.startTime, jobExecution.endTime)
        val durationHours = duration.seconds / 3600.0

        val stepExecutions = jobExecution.stepExecutions

        // 처리량
        val totalRead = stepExecutions.sumOf { it.readCount }
        val totalWrite = stepExecutions.sumOf { it.writeCount }

        // 🔥 인프라 비용 계산
        val ec2Cost = durationHours * ec2HourlyCost

        // 🔥 API 비용 추정 (실제로는 로그나 API 모니터링에서 가져와야 함)
        val estimatedSummaryCalls = totalWrite  // 각 post마다 요약
        val estimatedEmbeddingCalls = totalWrite  // 각 post마다 임베딩

        // 평균 post 길이 5000자 가정
        val avgPostLength = 5000
        val summaryCost = (estimatedSummaryCalls * avgPostLength / 1000.0) * geminiFlashSummaryCost
        val embeddingCost = (estimatedEmbeddingCalls * avgPostLength / 1000.0) * geminiEmbeddingCost

        val totalApiCost = summaryCost + embeddingCost
        val totalCost = ec2Cost + totalApiCost

        // 🔥 Cost per Record
        val costPerPost = if (totalWrite > 0) totalCost / totalWrite else 0.0

        log.info("""
            ========================================
            💰 COST ANALYSIS
            ========================================
            Job: ${jobExecution.jobInstance.jobName}
            Duration: ${String.format("%.4f", durationHours)}h

            📊 Processing Stats:
            - Total Posts Processed: $totalWrite
            - Total API Calls: ${estimatedSummaryCalls + estimatedEmbeddingCalls}

            💵 Infrastructure Cost:
            - EC2 t2.micro: $${String.format("%.6f", ec2Cost)} (${String.format("%.2f", durationHours)}h × $${ec2HourlyCost}/h)

            💵 API Cost (Estimated):
            - Gemini Summary: $${String.format("%.6f", summaryCost)}
            - Gemini Embedding: $${String.format("%.6f", embeddingCost)}
            - Total API: $${String.format("%.6f", totalApiCost)}

            💰 Total Cost:
            - Job Total: $${String.format("%.6f", totalCost)}
            - Cost per Post: $${String.format("%.8f", costPerPost)}

            📈 Monthly Projection (30 runs):
            - Infrastructure: $${String.format("%.2f", ec2Cost * 30)}
            - API: $${String.format("%.2f", totalApiCost * 30)}
            - Total: $${String.format("%.2f", totalCost * 30)}

            ⚠️  Cost Efficiency:
            ${analyzeCostEfficiency(costPerPost, durationHours, totalWrite)}
            ========================================
        """.trimIndent())

        // CSV 출력
        log.info("COST_CSV," +
                "${jobExecution.jobInstance.jobName}," +
                "${jobExecution.jobExecutionId}," +
                "${String.format("%.6f", totalCost)}," +
                "${String.format("%.8f", costPerPost)}," +
                "${totalWrite}," +
                "${String.format("%.4f", durationHours)}")
    }

    private fun analyzeCostEfficiency(
        costPerPost: Double,
        durationHours: Double,
        totalWrite: Long
    ): String {
        val issues = mutableListOf<String>()

        // 시간당 처리량
        val postsPerHour = if (durationHours > 0) totalWrite / durationHours else 0.0

        if (postsPerHour < 100) {
            issues.add("- 🔴 낮은 처리량 (${String.format("%.1f", postsPerHour)} posts/h) → 인프라 비용 비효율")
        }

        if (costPerPost > 0.001) {
            issues.add("- 🟡 Post당 비용 높음 ($${String.format("%.6f", costPerPost)}) → API 호출 최적화 필요")
        }

        return if (issues.isEmpty()) {
            "✅ 비용 효율적 (시간당 ${String.format("%.1f", postsPerHour)} posts 처리)"
        } else {
            issues.joinToString("\n            ")
        }
    }

    override fun beforeJob(jobExecution: JobExecution) {
        // 시작 시 비용 추정
        log.info("💰 Cost tracking started for ${jobExecution.jobInstance.jobName}")
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(CostAnalysisListener::class.java)
    }
}
```

---

## 📋 설정 통합

### Step 1: Listener 등록

```kotlin
// batch/src/main/kotlin/com/techinsights/batch/config/PostCrawlingBatchConfig.kt

@Configuration
class PostCrawlingBatchConfig (
  private val jobRepository: JobRepository,
  private val transactionManager: PlatformTransactionManager,
  private val companyReader: CompanyReader,
  private val skewAnalysisProcessor: SkewAnalysisProcessor,  // 변경
  private val rawPostWriter: RawPostWriter,
  private val properties: PostCrawlingBatchProperties,
  private val loggingJobExecutionListener: LoggingJobExecutionListener,
  private val resourceMetricsListener: ResourceMetricsListener,  // 추가
  private val costAnalysisListener: CostAnalysisListener  // 추가
){

  @Bean
  fun crawlPostJob(@Qualifier("crawlPostStep") crawlPostStep: Step): Job =
    JobBuilder(properties.jobName, jobRepository)
      .incrementer(RunIdIncrementer())
      .listener(loggingJobExecutionListener)
      .listener(resourceMetricsListener)  // 추가
      .listener(costAnalysisListener)  // 추가
      .listener(object : JobExecutionListener {
          override fun afterJob(jobExecution: JobExecution) {
              skewAnalysisProcessor.analyzeSkew()  // Skew 분석
          }
      })
      .start(crawlPostStep)
      .build()

  @Bean
  fun crawlPostStep(): Step = StepBuilder(properties.stepName, jobRepository)
    .chunk<CompanyDto, List<PostDto>>(properties.chunkSize, transactionManager)
    .reader(companyReader)
    .processor(skewAnalysisProcessor)  // 변경
    .writer(rawPostWriter)
    .faultTolerant()
    .retry(Exception::class.java)
    .retryLimit(properties.retryLimit)
    .skip(Exception::class.java)
    .skipLimit(10)
    .build()
}
```

---

## 🚀 실행 및 데이터 수집

### Step 1: 빌드 및 배포

```bash
./gradlew :batch:build

# 서버에 배포 (또는 로컬 실행)
```

### Step 2: 배치 실행

```bash
java -jar batch.jar --spring.batch.job.names=crawlPostJob
```

### Step 3: 로그 확인

```bash
tail -f /var/log/batch/batch.log | grep -E "RESOURCE_METRICS|SKEW|COST"
```

**예상 출력:**

```
========================================
📊 RESOURCE METRICS - Job Completed
========================================
Job: crawlPostJob
Duration: 892s

🖥️  CPU Metrics:
- Average CPU Load: 12.34%
- Efficiency: 🔴 IDLE (I/O bound)
- Available Cores: 1

💾 Memory Metrics:
- Peak Memory Used: 450 MB
- Average Memory: 380 MB
- Memory Utilization: 45.23%

🧵 Thread Metrics:
- Peak Threads: 25
- Waiting Threads: 18
- Thread Utilization: 72.00%

📈 Processing Metrics:
- Total Processed: 120 items
- Throughput: 0.13 items/sec
- Cost per Item (CPU time): 7.43s

⚠️  Bottleneck Analysis:
- 🔴 CPU Idle (12.3%) → I/O Bound (네트워크/DB 대기)
- 🔴 18/25 threads waiting → External I/O Bottleneck
========================================

========================================
📊 DATA SKEW ANALYSIS
========================================
Total Companies: 13

⏱️  Processing Time Distribution:
- Min: 15000ms (15s)
- P50 (Median): 45000ms (45s)
- Mean: 68615.38ms
- P95: 180000ms (180s)
- P99: 210000ms (210s)
- Max: 250000ms (250s)
- Std Dev: 62345.12ms

🎯 Skewness Analysis:
- Skewness: 1.23 🔴 Highly Right-Skewed (소수만 매우 느림)
- Max/Median Ratio: 5.56x
- Top 20% Contribution: 68.5%

🔴 Slowest Companies (Top 5):
1. Woowahan: 250000ms (250s)
2. Kakao: 180000ms (180s)
3. Naver: 120000ms (120s)
4. Toss: 60000ms (60s)
5. Kurly: 55000ms (55s)

🟢 Fastest Companies (Top 5):
1. CompanyA: 15000ms (15s)
2. CompanyB: 18000ms (18s)
3. CompanyC: 25000ms (25s)
4. CompanyD: 30000ms (30s)
5. CompanyE: 35000ms (35s)

⚠️  Recommendation:
- 🔴 Max/Median ratio 5.6x → 특정 회사가 전체 시간 지배
- 🔴 상위 20% 회사가 전체의 68.5% 차지 → 심각한 Data Skew
- 🟡 Right-Skewed 분포 → 소수 회사만 오래 걸림 → 병렬 처리 시 개선 효과 제한적
========================================

========================================
💰 COST ANALYSIS
========================================
Job: crawlPostJob
Duration: 0.2478h

📊 Processing Stats:
- Total Posts Processed: 120
- Total API Calls: 240

💵 Infrastructure Cost:
- EC2 t2.micro: $0.003370 (0.25h × $0.0136/h)

💵 API Cost (Estimated):
- Gemini Summary: $0.045000
- Gemini Embedding: $0.006000
- Total API: $0.051000

💰 Total Cost:
- Job Total: $0.054370
- Cost per Post: $0.00045308

📈 Monthly Projection (30 runs):
- Infrastructure: $0.10
- API: $1.53
- Total: $1.63

⚠️  Cost Efficiency:
- 🔴 낮은 처리량 (484.3 posts/h) → 인프라 비용 비효율
========================================
```

---

## 📊 데이터 분석 및 리포트

### 수집된 CSV 데이터 추출

```bash
# 리소스 메트릭
grep "RESOURCE_METRICS_CSV" /var/log/batch/batch.log > resource_metrics.csv

# Skew 데이터
grep "SKEW_CSV" /var/log/batch/batch.log > skew_data.csv

# 비용 데이터
grep "COST_CSV" /var/log/batch/batch.log > cost_data.csv
```

### Excel/Google Sheets 분석

**resource_metrics.csv:**
```csv
RESOURCE_METRICS_CSV,crawlPostJob,12345,892.5,12.34,380,25,18,120,0.13
```

피벗 테이블:
- X축: 실행 일자
- Y축: CPU 사용률, 처리량
- 그래프: 시간에 따른 CPU 사용률 추이

---

## 🎯 최종 리포트 예시

```markdown
# Batch System 성능 분석 리포트

## 1. 컴퓨팅 효율성 분석

### 현황
- **CPU 사용률**: 평균 12.3% (30회 측정)
- **메모리 사용률**: 평균 45.2%
- **스레드 대기 비율**: 72% (18/25 threads waiting)

### 🔴 Critical Finding: I/O Bound
**근거:**
- CPU가 12.3%만 사용되고 88%는 유휴 상태
- 25개 스레드 중 18개가 WAITING 상태
- 처리량 0.13 items/sec (목표 10 items/sec의 1.3%)

**원인:**
- 외부 API/네트워크 대기 시간이 전체의 88%
- 순차 처리로 인한 대기 시간 누적

**개선안:**
- 병렬 처리 도입 → CPU 사용률 70% 목표
- 예상 효과: 처리 시간 85% 단축

---

## 2. Data Skew 분석

### 현황
- **Max/Median 비율**: 5.56x
- **Skewness**: 1.23 (Right-Skewed)
- **상위 20% 기여도**: 68.5%

### 🔴 Critical Finding: 심각한 Data Skew
**근거:**
- Woowahan: 250초 vs 중간값 45초 (5.6배 차이)
- 상위 3개 회사(23%)가 전체 시간의 68.5% 차지
- 가장 느린 회사가 전체 배치 시간을 지배

**개선안:**
1. Woowahan 타임아웃 단축 (300초 → 30초)
   - 예상 효과: 전체 시간 220초 단축 (25%)
2. 병렬 처리로 느린 회사 격리
   - 예상 효과: 전체 시간 60% 단축

---

## 3. 비용 효율성 분석

### 현황
- **Post당 비용**: $0.000453
- **월간 총 비용**: $1.63 (30회 실행)
  - 인프라: $0.10
  - API: $1.53
- **시간당 처리량**: 484 posts/h

### 🟡 Warning: 낮은 처리량
**근거:**
- 목표 시간당 3600 posts vs 실제 484 posts (13% 수준)
- EC2가 15분 돌아서 $0.0034 vs 목표 2분 ($0.00045)
- **시간당 인프라 비용 7.5배 초과**

**개선 후 예상:**
- 처리 시간 15분 → 2분 (86% 단축)
- 월간 비용 $1.63 → $0.23 (86% 절감)
- **연간 $16.8 절감**
```

---

이제 **"느리다"가 아니라 "CPU 12%인데 왜 15분이나 걸리죠?"**라고 데이터로 말할 수 있습니다! 🎯
