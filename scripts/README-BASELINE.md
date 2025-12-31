# 배치 시스템 Baseline 데이터 수집 가이드

## 🎯 목적

배치 시스템 개선 작업을 시작하기 전에 **현재 상태(Baseline)**를 정확히 측정하고 기록합니다.
이를 통해:
1. 개선의 근거를 명확히 제시
2. 개선 전/후 비교 가능
3. ROI(투자 대비 효과) 증명

---

## 📁 제공되는 도구

### 1. SQL 쿼리 파일

| 파일 | 용도 | 실행 시간 |
|------|------|----------|
| `export_batch_metadata.sql` | Spring Batch 메타데이터 분석 | ~30초 |
| `analyze_company_performance.sql` | 회사별 크롤링 성능 분석 | ~10초 |

### 2. 쉘 스크립트

| 파일 | 용도 | 실행 시간 |
|------|------|----------|
| `quick_baseline_check.sh` | 빠른 현황 체크 (Health Check) | ~5초 |

### 3. 마크다운 문서

| 파일 | 용도 |
|------|------|
| `batch-baseline-analysis.md` | 상세 분석 방법 및 코드 템플릿 |

---

## 🚀 빠른 시작 (5분 완성)

### Step 1: 데이터베이스 접속 정보 설정

```bash
# 환경 변수 설정
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=techinsights
export DB_USER=postgres
export DB_PASSWORD=your_password
```

또는 `.env` 파일 생성:
```bash
# .env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=techinsights
DB_USER=postgres
DB_PASSWORD=your_password
```

```bash
source .env
```

### Step 2: 빠른 현황 체크

```bash
cd /Users/kitoha/.claude-worktrees/TechInsights-Server/blissful-swartz

./scripts/quick_baseline_check.sh
```

**출력 예시:**
```
========================================
📊 Batch System Quick Baseline Check
========================================

✅ Database connection successful

1️⃣  Recent Batch Executions (Last 7 days)
--------------------------------------------
job_name              | runs | success | failed | last_run
---------------------|------|---------|--------|------------------
crawlPostJob         |   7  |    5    |    2   | 2024-01-15 09:00

2️⃣  Average Execution Time
--------------------------------------------
job_name              | avg_seconds | avg_minutes | max_seconds | max_minutes
---------------------|-------------|-------------|-------------|------------
crawlPostJob         |   450.23    |    7.50     |  1200.50    |   20.01

...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Overall Health Score: 65/100
Status: ⚡ GOOD (Minor improvements needed)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Step 3: 상세 데이터 추출

```bash
# Spring Batch 메타데이터 추출
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME \
  -f scripts/export_batch_metadata.sql \
  > baseline_batch_metadata.txt

# 회사별 성능 데이터 추출
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME \
  -f scripts/analyze_company_performance.sql \
  > baseline_company_performance.txt
```

---

## 📊 상세 데이터 수집 (프로덕션 환경)

### 전제 조건

- [ ] 배치가 최소 7일 이상 실행된 이력이 있어야 함
- [ ] PostgreSQL 접근 권한 필요
- [ ] (선택) 로그 파일 접근 권한

### Phase 1: 사전 측정 (현재 상태)

#### 1-1. 데이터베이스에서 메트릭 추출

```bash
# 날짜별 디렉토리 생성
REPORT_DIR="./baseline-reports/$(date +%Y%m%d)"
mkdir -p "$REPORT_DIR"

# 배치 메타데이터
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME \
  -f scripts/export_batch_metadata.sql \
  > "$REPORT_DIR/batch_metadata.txt"

# 회사별 성능
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME \
  -f scripts/analyze_company_performance.sql \
  > "$REPORT_DIR/company_performance.txt"

# CSV 형식으로도 추출 (Excel 분석용)
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME \
  -c "COPY (
    SELECT
      ji.job_name,
      je.job_execution_id,
      je.start_time,
      je.end_time,
      EXTRACT(EPOCH FROM (je.end_time - je.start_time)) as duration_seconds,
      je.status,
      se.read_count,
      se.write_count,
      se.skip_count
    FROM batch_job_execution je
    JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
    LEFT JOIN batch_step_execution se ON je.job_execution_id = se.job_execution_id
    WHERE je.start_time >= NOW() - INTERVAL '30 days'
    ORDER BY je.start_time DESC
  ) TO STDOUT WITH CSV HEADER" \
  > "$REPORT_DIR/batch_executions.csv"

echo "✅ Data exported to $REPORT_DIR"
```

#### 1-2. 로그 파일 분석 (선택사항)

```bash
# 로그 파일 위치 확인
LOG_FILE="/var/log/batch/batch.log"

# 최근 배치 실행 로그 추출
grep -A 50 "Batch.*시작" "$LOG_FILE" | tail -n 1000 > "$REPORT_DIR/recent_logs.txt"

# 에러 로그만 추출
grep -E "ERROR|FAILED|Exception" "$LOG_FILE" > "$REPORT_DIR/errors.txt"

# 회사별 처리 시간 (만약 로그에 기록되어 있다면)
grep "Processing:" "$LOG_FILE" | grep -o "Company: [^,]*" | sort | uniq -c > "$REPORT_DIR/company_mentions.txt"
```

### Phase 2: 성능 측정 도구 추가 (선택사항)

더 상세한 데이터를 원한다면 `batch-baseline-analysis.md`에 있는 `BaselineMetricsListener`를 프로젝트에 추가하세요.

```bash
# 1. 파일 복사 (batch-baseline-analysis.md에서 코드 복사)
# batch/src/main/kotlin/com/techinsights/batch/listener/BaselineMetricsListener.kt

# 2. 빌드
./gradlew :batch:build

# 3. 배포 및 실행
# ... 배포 프로세스에 따라 진행

# 4. 로그 확인
tail -f /var/log/batch/batch.log | grep "BASELINE"
```

---

## 📈 수집해야 할 핵심 메트릭

### 1. 성능 메트릭

| 메트릭 | 측정 방법 | 목표 값 | 현재 값 |
|--------|----------|---------|---------|
| 전체 배치 소요 시간 (평균) | `export_batch_metadata.sql` → "Avg Duration" | < 30분 | ______분 |
| 전체 배치 소요 시간 (최대) | `export_batch_metadata.sql` → "Max Duration" | < 45분 | ______분 |
| 회사당 평균 처리 시간 | 로그 분석 또는 추정 | < 2분 | ______분 |
| 처리량 (throughput) | "Avg Write" / "Avg Duration" | > 10 items/sec | _____ items/sec |

### 2. 안정성 메트릭

| 메트릭 | 측정 방법 | 목표 값 | 현재 값 |
|--------|----------|---------|---------|
| 배치 성공률 (7일) | `quick_baseline_check.sh` | > 95% | ______% |
| Skip 비율 | "Total Skipped" / "Total Read" | < 5% | ______% |
| 실패한 회사 수 | `analyze_company_performance.sql` | 0개 | ______개 |

### 3. 데이터 품질 메트릭

| 메트릭 | 측정 방법 | 목표 값 | 현재 값 |
|--------|----------|---------|---------|
| 요약 완료율 | `quick_baseline_check.sh` | > 90% | ______% |
| 임베딩 완료율 | `quick_baseline_check.sh` | > 90% | ______% |
| 데이터 신선도 (7일 이내) | `analyze_company_performance.sql` → "Freshness" | 100% | ______% |
| 중복 데이터 | `analyze_company_performance.sql` → "Duplicate URL Check" | 0개 | ______개 |

---

## 📋 Baseline 리포트 작성

수집한 데이터를 바탕으로 다음 형식의 리포트를 작성하세요.

### 템플릿

```markdown
# Batch System Baseline Report

**측정 기간:** YYYY-MM-DD ~ YYYY-MM-DD
**측정일:** YYYY-MM-DD
**측정자:** [이름]

## 1. Executive Summary

- **전체 배치 실행 횟수:** ___회
- **평균 성공률:** ___%
- **평균 실행 시간:** ___분
- **총 처리 게시글 수:** ___개

## 2. 성능 메트릭

### 2.1 Job 실행 시간

| Job | 평균 | 최대 | 최소 | 목표 | 평가 |
|-----|------|------|------|------|------|
| crawlPostJob | ___분 | ___분 | ___분 | 30분 | ⚠️ 초과 |
| summarizePostJob | ___분 | ___분 | ___분 | 60분 | ✅ 양호 |

### 2.2 회사별 처리 현황

| 회사 | 게시글 수 | 마지막 수집 | 데이터 나이 | 상태 |
|------|-----------|-------------|-------------|------|
| Woowahan | ___ | YYYY-MM-DD | _일 전 | ✅ Fresh |
| Kakao | ___ | YYYY-MM-DD | _일 전 | ⚠️ Stale |

## 3. 문제점 식별

### 3.1 성능 병목

1. **순차 처리로 인한 지연**
   - 현황: 13개 회사를 순차 처리
   - 영향: 가장 느린 회사(___분)가 전체 시간에 영향
   - 근거: [SQL 쿼리 결과 참조]

2. **타임아웃 시간 과다**
   - 현황: Rate Limiter timeout 300초
   - 영향: 한 회사 지연 시 최대 5분 대기
   - 근거: [application.yml 설정]

### 3.2 안정성 이슈

1. **실패 재실행 메커니즘 부재**
   - 현황: 실패한 회사 목록만 로그에 기록
   - 영향: 재실행 시 전체 회사 재처리
   - 근거: [코드 분석]

### 3.3 모니터링 공백

1. **실시간 진행률 알 수 없음**
   - 현황: 로그 파일에서만 확인 가능
   - 영향: 운영자가 상태 파악 어려움
   - 근거: [현재 Listener 코드]

## 4. 개선 우선순위

### High Priority
1. 병렬 처리 도입 → 예상 효과: 전체 시간 60% 단축
2. 실패 추적 테이블 → 예상 효과: 재실행 시간 90% 단축

### Medium Priority
3. SLA 모니터링 → 예상 효과: 성능 저하 조기 발견
4. 실시간 진행률 표시 → 예상 효과: 운영 효율성 향상

## 5. 첨부 자료

- [batch_metadata.txt](./baseline-reports/20240115/batch_metadata.txt)
- [company_performance.txt](./baseline-reports/20240115/company_performance.txt)
- [batch_executions.csv](./baseline-reports/20240115/batch_executions.csv)
```

---

## 🔄 지속적 모니터링

개선 작업 후에도 동일한 스크립트를 사용하여 정기적으로 측정하세요.

### 주간 체크

```bash
# 매주 월요일 오전 실행
0 9 * * 1 /path/to/quick_baseline_check.sh >> /var/log/batch/weekly_health.log
```

### 월간 상세 리포트

```bash
# 매월 1일 실행
./scripts/monthly_report.sh
```

**monthly_report.sh:**
```bash
#!/bin/bash
MONTH=$(date +%Y%m)
REPORT_DIR="./monthly-reports/$MONTH"
mkdir -p "$REPORT_DIR"

# 데이터 추출
psql ... -f scripts/export_batch_metadata.sql > "$REPORT_DIR/batch.txt"
psql ... -f scripts/analyze_company_performance.sql > "$REPORT_DIR/company.txt"

# 이메일 또는 Slack 전송
# ...
```

---

## 📊 데이터 시각화 (선택사항)

### Excel/Google Sheets

1. `batch_executions.csv` 파일 열기
2. 피벗 테이블 생성:
   - 행: `job_name`
   - 값: `AVG(duration_seconds)`, `COUNT(*)`
3. 차트 삽입:
   - 꺾은선 그래프: 일별 실행 시간 추이
   - 막대 그래프: Job별 평균 시간 비교

### Python/Pandas (선택)

```python
import pandas as pd
import matplotlib.pyplot as plt

# CSV 로드
df = pd.read_csv('baseline-reports/20240115/batch_executions.csv')

# 일별 평균 실행 시간
daily = df.groupby(df['start_time'].str[:10])['duration_seconds'].mean()
daily.plot(title='Daily Average Execution Time')
plt.ylabel('Seconds')
plt.savefig('daily_trend.png')

# Job별 성공률
success_rate = df.groupby('job_name')['status'].apply(
    lambda x: (x == 'COMPLETED').sum() / len(x) * 100
)
success_rate.plot(kind='bar', title='Success Rate by Job')
plt.ylabel('Success %')
plt.savefig('success_rate.png')
```

---

## ❓ FAQ

### Q1. 배치 실행 이력이 없으면 어떻게 하나요?

**A:** 최소 7일간 배치를 실행하여 이력을 쌓은 후 측정하세요. 급한 경우 3일치 데이터로도 가능하지만 정확도가 떨어집니다.

### Q2. PostgreSQL 접근 권한이 없으면?

**A:** 다음 대안을 사용하세요:
1. DBA에게 SQL 쿼리 실행 요청
2. 애플리케이션 로그 파일 분석
3. `BaselineMetricsListener` 추가하여 로그로 메트릭 수집

### Q3. 회사별 처리 시간을 어떻게 측정하나요?

**A:** 현재는 직접 측정 불가능하므로:
1. `BaselineMetricsListener` 추가 (권장)
2. 또는 전체 시간 ÷ 회사 수로 추정
3. 개선 후에는 상세 측정 가능

### Q4. 얼마나 오래 측정해야 하나요?

**A:**
- **최소:** 7일 (1주일 주기 파악)
- **권장:** 14~30일 (월간 패턴 파악)
- **이상적:** 60~90일 (계절성 파악)

### Q5. 개선 전/후 비교는 어떻게 하나요?

**A:** 동일한 SQL 쿼리를 개선 후 다시 실행하여 결과를 비교:
```bash
# 개선 전
./scripts/export_batch_metadata.sql > before_improvement.txt

# 개선 작업 수행
# ...

# 개선 후 (7일 후)
./scripts/export_batch_metadata.sql > after_improvement.txt

# 비교
diff before_improvement.txt after_improvement.txt
```

---

## 📞 도움이 필요하시면

- SQL 쿼리 오류: PostgreSQL 로그 확인 (`/var/log/postgresql/`)
- 스크립트 권한 오류: `chmod +x scripts/*.sh`
- 데이터베이스 연결 실패: `pg_hba.conf` 확인

---

## ✅ 체크리스트

준비 완료 여부를 체크하세요:

- [ ] PostgreSQL 접근 가능
- [ ] 배치 실행 이력 7일 이상 확보
- [ ] 모든 스크립트 실행 권한 부여
- [ ] 데이터 저장 디렉토리 생성
- [ ] `quick_baseline_check.sh` 실행 성공
- [ ] SQL 쿼리 2개 실행 성공
- [ ] 결과 파일 생성 확인
- [ ] Baseline 리포트 작성 완료

모든 항목이 체크되었다면 개선 작업을 시작할 준비가 완료되었습니다! 🎉
