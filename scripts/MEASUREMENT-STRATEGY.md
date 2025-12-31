# 배치 시스템 측정 전략 가이드

## 🎯 핵심 질문: "지금 당장 측정해도 되나요?"

**답변: 상황에 따라 다릅니다.**

---

## 📊 측정 데이터의 신뢰도

### 배치 실행 이력에 따른 데이터 신뢰도

| 실행 횟수 | 신뢰도 | 용도 | 권장 사항 |
|-----------|--------|------|-----------|
| **0-2회** | ❌ 매우 낮음 | 사용 불가 | 최소 1주일 실행 필요 |
| **3-6회** | ⚠️ 낮음 | 경향 파악만 가능 | 참고용으로만 사용 |
| **7-13회** | 🟡 보통 | Baseline 가능 (주의 필요) | 이상치 제거 후 사용 |
| **14-29회** | ✅ 높음 | Baseline 적합 | 권장 |
| **30회 이상** | 🎯 매우 높음 | 정확한 통계 | 이상적 |

### 왜 최소 7회 이상이어야 하나?

1. **통계적 유의성**
   - 평균: 최소 5-7개 샘플 필요
   - 표준편차: 의미있는 계산을 위해 최소 7개
   - 이상치 제거: 최소 10개 (상위/하위 각 1개 제거 후에도 충분)

2. **주간 패턴 파악**
   - 요일별 차이 (평일 vs 주말)
   - 시간대별 부하
   - 데이터 변동성

3. **신뢰구간**
   - 7회: ±30% 오차
   - 14회: ±20% 오차
   - 30회: ±10% 오차

---

## 🔍 현재 상황 진단

### Step 1: 현재 실행 이력 확인

```bash
# PostgreSQL에 접속하여 확인
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=techinsights
export DB_USER=postgres

psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME << 'EOF'
-- 배치 실행 이력 확인
SELECT
    ji.job_name,
    COUNT(*) as total_runs,
    MIN(je.start_time) as first_run,
    MAX(je.start_time) as last_run,
    EXTRACT(DAY FROM (MAX(je.start_time) - MIN(je.start_time))) as days_span
FROM batch_job_execution je
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
GROUP BY ji.job_name
ORDER BY ji.job_name;
EOF
```

**예상 출력 해석:**

```
job_name         | total_runs | first_run           | last_run            | days_span
-----------------|------------|---------------------|---------------------|----------
crawlPostJob     |     2      | 2024-01-15 09:00:00 | 2024-01-16 09:00:00 |    1
```

**해석:**
- total_runs = 2 → ❌ **데이터 부족, 측정 불가**
- days_span = 1 → ⚠️ **단기 데이터만 존재**

---

## 🚦 상황별 대응 전략

### 시나리오 1: 로컬 개발 환경 (배치 이력 0-5회)

#### ❌ 하지 말아야 할 것
- ~~Baseline 리포트 작성~~ → 무의미한 데이터
- ~~평균/표준편차 계산~~ → 통계적 의미 없음
- ~~성능 개선 근거로 사용~~ → 신뢰도 부족

#### ✅ 대신 할 수 있는 것

**Option 1: 프로덕션 데이터 활용 (권장)**

```bash
# 1. 프로덕션 DB에 접속
export DB_HOST=production-db.example.com
export DB_USER=readonly_user

# 2. 프로덕션 데이터로 Baseline 측정
./scripts/quick_baseline_check.sh

# 3. 상세 데이터 추출
psql -h $DB_HOST ... -f scripts/export_batch_metadata.sql > production_baseline.txt
```

**장점:**
- ✅ 실제 운영 환경 데이터
- ✅ 충분한 샘플 수
- ✅ 실제 부하 반영

**단점:**
- ⚠️ 프로덕션 DB 접근 권한 필요
- ⚠️ 읽기 전용 권한이라도 DBA 승인 필요

---

**Option 2: 코드 정적 분석으로 문제점 파악**

실행 이력 없이도 현재 코드에서 파악 가능한 문제점:

```bash
# 분석 스크립트 실행
./scripts/analyze_code_issues.sh
```

생성할 스크립트 내용:

```bash
#!/bin/bash
echo "========================================="
echo "🔍 배치 코드 정적 분석"
echo "========================================="
echo ""

echo "1️⃣  병렬 처리 여부 확인"
echo "----------------------------------------"
if grep -r "partitioner\|gridSize\|TaskExecutor" batch/src/main/kotlin/; then
    echo "✅ 병렬 처리 구현됨"
else
    echo "❌ 병렬 처리 미구현 → 순차 처리로 추정"
    echo "   근거: Partitioner 또는 TaskExecutor 사용 흔적 없음"
fi
echo ""

echo "2️⃣  타임아웃 설정 확인"
echo "----------------------------------------"
echo "Rate Limiter 타임아웃:"
grep -A 2 "timeout" batch/src/main/resources/application.yml | grep "timeout-seconds"
echo ""

echo "3️⃣  실패 추적 메커니즘 확인"
echo "----------------------------------------"
if find batch/src -name "*Failure*.kt" | grep -q .; then
    echo "✅ 실패 추적 코드 발견"
else
    echo "❌ 실패 추적 테이블/로직 없음"
    echo "   근거: Failure 관련 클래스 없음"
fi
echo ""

echo "4️⃣  SLA 설정 확인"
echo "----------------------------------------"
if grep -r "sla\|SLA" batch/src/main/resources/application.yml; then
    echo "✅ SLA 설정 발견"
else
    echo "❌ SLA 정의 없음"
    echo "   근거: application.yml에 sla 관련 설정 없음"
fi
echo ""

echo "5️⃣  Freshness 추적 확인"
echo "----------------------------------------"
if grep -r "lastCrawledAt\|last_crawled_at" domain/src/; then
    echo "✅ Freshness 추적 필드 존재"
else
    echo "❌ Freshness 추적 미구현"
    echo "   근거: Company 엔티티에 lastCrawledAt 필드 없음"
fi
echo ""

echo "✅ 정적 분석 완료"
```

**이 방법으로 얻을 수 있는 것:**
- ✅ 아키텍처 수준의 문제점 (병렬 처리 여부 등)
- ✅ 설정 값 검토 (타임아웃 등)
- ✅ 누락된 기능 파악 (실패 추적, SLA 등)

**얻을 수 없는 것:**
- ❌ 실제 성능 수치 (평균 시간, 처리량 등)
- ❌ 실패율, Skip 비율
- ❌ 회사별 처리 시간

---

**Option 3: 단일 실행 상세 모니터링**

배치를 **1회만** 실행하되, 최대한 상세히 측정:

```kotlin
// BaselineMetricsListener를 추가하여 1회 실행
// batch/src/main/kotlin/com/techinsights/batch/listener/SingleRunProfiler.kt

@Component
class SingleRunProfiler : JobExecutionListener, StepExecutionListener {

    private val companyTimes = mutableMapOf<String, Long>()

    override fun beforeJob(jobExecution: JobExecution) {
        log.info("""
            ==========================================
            📊 SINGLE RUN PROFILING - START
            ==========================================
            Job: ${jobExecution.jobInstance.jobName}
            Started: ${LocalDateTime.now()}

            이 실행은 상세 프로파일링 모드입니다.
            모든 회사별 처리 시간을 개별 측정합니다.
            ==========================================
        """.trimIndent())
    }

    // ... (batch-baseline-analysis.md의 코드 참고)

    override fun afterJob(jobExecution: JobExecution) {
        // 회사별 처리 시간 출력
        log.info("""
            ==========================================
            📊 SINGLE RUN PROFILING - RESULTS
            ==========================================

            회사별 처리 시간:
            ${companyTimes.entries.sortedByDescending { it.value }
                .joinToString("\n") { (company, ms) ->
                    "$company: ${ms}ms (${ms/1000}s)"
                }}

            가장 느린 회사: ${companyTimes.maxByOrNull { it.value }?.key} (${companyTimes.values.maxOrNull()?.div(1000)}s)
            가장 빠른 회사: ${companyTimes.minByOrNull { it.value }?.key} (${companyTimes.values.minOrNull()?.div(1000)}s)
            평균 처리 시간: ${companyTimes.values.average().toLong()/1000}s
            전체 소요 시간: ${duration.seconds}s

            ⚠️  주의: 이것은 1회 실행 결과입니다.
            통계적 신뢰도를 위해서는 최소 7회 실행 필요.
            ==========================================
        """.trimIndent())
    }
}
```

**실행 방법:**
```bash
# 1. 코드 추가
# 2. 빌드
./gradlew :batch:build

# 3. 실행 (로그 레벨 DEBUG)
java -jar batch/build/libs/batch.jar \
  --spring.batch.job.names=crawlPostJob \
  --logging.level.com.techinsights.batch=DEBUG \
  | tee single_run_profile.log

# 4. 결과 확인
grep "SINGLE RUN PROFILING" single_run_profile.log
```

**얻을 수 있는 것:**
- ✅ 회사별 처리 시간 (1회 측정값)
- ✅ 전체 배치 소요 시간
- ✅ 병목 회사 식별
- ✅ 대략적인 평균값

**제한 사항:**
- ⚠️ 통계적 신뢰도 낮음
- ⚠️ 네트워크 일시 지연 등 이상치에 취약
- ⚠️ "평균"이라고 말하기 어려움

**사용 방법:**
- "1회 측정 결과, 가장 느린 회사는 X였으며 Y초 소요"
- "참고치로, 전체 배치는 Z분 걸림"
- "정확한 측정을 위해 1주일간 데이터 수집 예정"

---

### 시나리오 2: 프로덕션 환경 (배치 이력 7-13회)

#### ✅ 할 수 있는 것

```bash
# 1. 빠른 체크
./scripts/quick_baseline_check.sh

# 2. 상세 데이터 추출
./scripts/export_batch_metadata.sql
./scripts/analyze_company_performance.sql
```

#### ⚠️ 주의사항

**데이터 품질 체크:**

```sql
-- 이상치(outlier) 확인
SELECT
    ji.job_name,
    je.job_execution_id,
    EXTRACT(EPOCH FROM (je.end_time - je.start_time)) as duration_sec,
    -- Z-score 계산 (평균에서 얼마나 벗어났는지)
    ABS(
        EXTRACT(EPOCH FROM (je.end_time - je.start_time)) -
        AVG(EXTRACT(EPOCH FROM (je.end_time - je.start_time))) OVER (PARTITION BY ji.job_name)
    ) / NULLIF(STDDEV(EXTRACT(EPOCH FROM (je.end_time - je.start_time))) OVER (PARTITION BY ji.job_name), 0) as z_score
FROM batch_job_execution je
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
WHERE je.end_time IS NOT NULL
ORDER BY z_score DESC
LIMIT 10;
```

**해석:**
- z_score > 2: 이상치 가능성 높음 (평균에서 표준편차 2배 이상 벗어남)
- z_score > 3: 명백한 이상치

**대응:**
```sql
-- 이상치를 제외한 평균 계산
WITH filtered_data AS (
    SELECT
        ji.job_name,
        EXTRACT(EPOCH FROM (je.end_time - je.start_time)) as duration_sec
    FROM batch_job_execution je
    JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
    WHERE je.end_time IS NOT NULL
)
SELECT
    job_name,
    ROUND(AVG(duration_sec), 2) as avg_with_outliers,
    ROUND(AVG(duration_sec) FILTER (
        WHERE duration_sec BETWEEN
            PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY duration_sec)
            AND PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY duration_sec)
    ), 2) as avg_without_outliers,
    COUNT(*) as total_samples,
    COUNT(*) FILTER (
        WHERE duration_sec BETWEEN
            PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY duration_sec)
            AND PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY duration_sec)
    ) as samples_used
FROM filtered_data
GROUP BY job_name;
```

---

### 시나리오 3: 프로덕션 환경 (배치 이력 30회 이상)

#### 🎯 이상적인 상황

```bash
# 모든 도구를 자유롭게 사용 가능
./scripts/quick_baseline_check.sh
./scripts/export_batch_metadata.sql
./scripts/analyze_company_performance.sql

# 추가 분석
# - 추세 분석 (시간에 따라 성능이 저하되는가?)
# - 계절성 분석 (특정 요일/시간대에 느린가?)
# - 상관관계 분석 (게시글 수와 처리 시간의 관계)
```

**신뢰구간 계산:**

```sql
SELECT
    job_name,
    ROUND(AVG(duration_sec), 2) as mean,
    ROUND(STDDEV(duration_sec), 2) as std_dev,
    -- 95% 신뢰구간
    ROUND(AVG(duration_sec) - 1.96 * STDDEV(duration_sec) / SQRT(COUNT(*)), 2) as ci_lower,
    ROUND(AVG(duration_sec) + 1.96 * STDDEV(duration_sec) / SQRT(COUNT(*)), 2) as ci_upper,
    COUNT(*) as sample_size
FROM (
    SELECT
        ji.job_name,
        EXTRACT(EPOCH FROM (je.end_time - je.start_time)) as duration_sec
    FROM batch_job_execution je
    JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
    WHERE je.end_time IS NOT NULL
) t
GROUP BY job_name;
```

**리포트 작성 시:**
- "평균 배치 시간은 45분 (95% CI: 40-50분)"
- "30회 측정 결과, 표준편차 5분"
- "통계적으로 유의미한 데이터 (n=30)"

---

## 💡 권장 전략

### 🏃 빠른 시작 (지금 당장)

**현재 로컬 환경이라면:**

```bash
# 1. 코드 정적 분석
cd /Users/kitoha/.claude-worktrees/TechInsights-Server/blissful-swartz

echo "=== 병렬 처리 확인 ==="
grep -r "partitioner\|gridSize" batch/src/main/kotlin/ || echo "❌ 병렬 처리 미구현"

echo ""
echo "=== 타임아웃 설정 ==="
grep "timeout-seconds" batch/src/main/resources/application.yml

echo ""
echo "=== 실패 추적 ==="
find batch/src -name "*Failure*" -o -name "*failure*" | head -5 || echo "❌ 실패 추적 미구현"

echo ""
echo "=== Company Freshness 필드 ==="
grep -r "lastCrawledAt" domain/src/ || echo "❌ Freshness 추적 미구현"
```

**결과:**
- 코드 레벨의 문제점 즉시 파악
- 개선 항목 리스트 작성 가능
- **정량적 성능 데이터 없이도** 개선 근거 제시 가능

---

### 📅 1주일 플랜 (정확한 데이터 필요 시)

**Day 1: 측정 준비**
```bash
# BaselineMetricsListener 코드 추가
# batch/src/main/kotlin/com/techinsights/batch/listener/BaselineMetricsListener.kt
# (batch-baseline-analysis.md 참고)

./gradlew :batch:build
```

**Day 2-8: 매일 배치 실행**
```bash
# 매일 같은 시간에 실행 (cron 설정)
0 9 * * * cd /path/to/project && java -jar batch.jar --spring.batch.job.names=crawlPostJob
```

**Day 9: 데이터 수집 및 분석**
```bash
./scripts/quick_baseline_check.sh
./scripts/export_batch_metadata.sql
./scripts/analyze_company_performance.sql

# Baseline 리포트 작성
```

---

### 🚀 프로덕션 데이터 활용 (가장 권장)

```bash
# 1. DBA에게 읽기 전용 계정 요청
# 2. 프로덕션 DB에서 데이터 추출
export DB_HOST=prod-db.techinsights.shop
export DB_USER=readonly

# 3. 즉시 Baseline 측정
./scripts/quick_baseline_check.sh > production_baseline_$(date +%Y%m%d).txt
./scripts/export_batch_metadata.sql > production_detailed_$(date +%Y%m%d).txt

# 4. 리포트 작성
```

---

## 📋 리포트 작성 가이드 (데이터 부족 시)

### ❌ 잘못된 예시

```markdown
## 성능 분석

- 평균 배치 시간: 10분 (2회 측정)
- 처리량: 100 posts/min

→ 샘플 수가 너무 적어 신뢰할 수 없음
```

### ✅ 올바른 예시 (데이터 부족 시)

```markdown
## 현재 상황 분석

### 정성적 분석 (코드 리뷰)

1. **순차 처리 아키텍처**
   - 근거: PostCrawlingBatchConfig.kt:43에서 단일 Step으로 구성
   - 영향: 13개 회사를 순차 처리, 병렬화 불가능
   - 개선 방향: Partitioning 또는 Multi-threading 도입

2. **과도한 타임아웃**
   - 근거: application.yml:62 timeout-seconds: 300
   - 영향: 한 회사 지연 시 최대 5분 대기
   - 개선 방향: 30초로 단축 + Retry 정책 개선

3. **실패 추적 메커니즘 부재**
   - 근거: Failure 관련 Entity/Repository 없음
   - 영향: 재실행 시 전체 회사 재처리
   - 개선 방향: BatchCrawlFailure 테이블 도입

### 정량적 추정 (1회 실행 결과)

⚠️ **주의: 다음 수치는 1회 실행 결과이며, 통계적 신뢰도가 낮습니다.**

- 전체 배치 소요 시간: 12분 (1회 측정)
- 가장 느린 회사: Woowahan (3분)
- 가장 빠른 회사: Toss (30초)

**향후 계획:**
- 1주일간 매일 실행하여 정확한 Baseline 측정 예정
- 또는 프로덕션 데이터 활용 검토
```

---

## 🎯 결론 및 Action Items

### 현재 로컬 환경 (배치 이력 부족)이라면:

#### ✅ 지금 할 수 있는 것
1. **코드 정적 분석** → 즉시 실행
2. **1회 상세 프로파일링** → BaselineMetricsListener 추가 후 1회 실행
3. **프로덕션 데이터 요청** → DBA에게 읽기 권한 요청

#### ⏳ 시간이 필요한 것
1. **1주일 플랜** → 로컬에서 매일 실행 (최소 7회)
2. **정확한 통계** → 30회 이상 실행

#### ❌ 지금 하지 말아야 할 것
1. ~~2-3회 데이터로 "평균"이라고 주장~~
2. ~~신뢰구간 계산~~
3. ~~통계적 유의성 주장~~

---

### 추천 방법 (우선순위)

1. **🥇 프로덕션 데이터 활용** (가능하다면)
   - 즉시 신뢰할 수 있는 데이터 확보
   - 실제 운영 환경 반영

2. **🥈 코드 정적 분석 + 1회 프로파일링**
   - 즉시 시작 가능
   - 개선 방향 파악에 충분

3. **🥉 1주일 데이터 수집**
   - 시간 소요
   - 하지만 가장 정확한 로컬 데이터

---

지금 상황에서는 **코드 정적 분석으로 시작**하는 것을 강력히 추천합니다!

```bash
# 지금 바로 실행 가능
cd /Users/kitoha/.claude-worktrees/TechInsights-Server/blissful-swartz
./scripts/analyze_code_issues.sh  # (다음 메시지에서 생성하겠습니다)
```
