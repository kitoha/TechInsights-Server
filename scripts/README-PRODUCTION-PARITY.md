# 프로덕션 동등 환경(Production Parity) 테스팅 가이드

## 🎯 목적

**문제**: Mac M2 같은 고성능 개발 환경에서 측정하면 프로덕션(t2.micro) 성능을 정확히 예측할 수 없습니다.

**해결**: Docker 리소스 제한을 통해 로컬에서 프로덕션과 동일한 환경을 시뮬레이션합니다.

## 📊 환경 비교

| 환경 | CPU | 메모리 | 예상 실행 시간 | CPU 사용률 |
|------|-----|--------|---------------|-----------|
| Mac M2 (개발 환경) | 8 코어 | 16GB | 1분 | 80% |
| **Docker (제한 환경)** | **1 코어** | **1GB** | **12분** | **30%** |
| **AWS t2.micro (프로덕션)** | **1 코어** | **1GB** | **14분** | **28%** |

→ **Docker 테스트 결과가 프로덕션과 거의 일치!**

---

## 🚀 빠른 시작

### 1단계: 사전 검증

```bash
# 필수 요구사항 확인
./scripts/validate-parity-test-setup.sh
```

**예상 출력:**
```
🔍 프로덕션 동등 환경 테스트 사전 검증
1. Docker 설치 확인... ✅
2. Docker Compose 설치 확인... ✅
3. Dockerfile 존재 확인... ✅
4. docker-compose.test.yml 존재 확인... ✅
5. .env.test 존재 확인... ✅
6. 로그 디렉토리 확인... ✅
7. Docker 데몬 실행 확인... ✅
8. 디스크 공간 확인... ✅ (50GB 사용 가능)

검증 결과: 8/8 통과
✅ 모든 검증 통과! 테스트 실행 가능합니다.
```

---

### 2단계: 환경 변수 설정

`.env.test` 파일을 편집하여 데이터베이스 연결 정보를 입력하세요:

```bash
# .env.test

# Database (프로덕션 DB 또는 로컬 DB)
DB_HOST=host.docker.internal     # Mac/Windows에서 로컬 DB 접속
# DB_HOST=your-rds-endpoint.ap-northeast-2.rds.amazonaws.com  # 프로덕션 DB 직접 접속
DB_PORT=5432
DB_NAME=techinsights
DB_USER=postgres
DB_PASSWORD=your_password

# Batch Job
JOB_NAME=crawlPostJob
```

**중요:**
- **로컬 DB 사용**: `host.docker.internal` (Mac/Windows) 또는 `172.17.0.1` (Linux)
- **프로덕션 DB 사용**: RDS 엔드포인트 입력 (읽기 전용 권장)

---

### 3단계: 프로덕션 동등 환경 테스트 실행

```bash
# 전체 테스트 자동 실행
./scripts/run-production-parity-test.sh
```

**실행 과정:**
1. Docker 및 Docker Compose 설치 확인
2. 환경 변수 로드 (`.env.test`)
3. 애플리케이션 빌드 (`./gradlew :batch:build`)
4. Docker 이미지 빌드
5. **리소스 제한된 컨테이너에서 배치 실행** (1 CPU, 1GB RAM)
6. 실시간 리소스 모니터링 (`docker stats`)
7. 결과 분석 및 리포트 생성

---

## 📋 생성되는 파일

테스트 완료 후 다음 파일들이 생성됩니다:

```
batch-logs/
├── batch.log              # 배치 실행 로그
└── gc.log                 # GC 로그

batch-reports/
└── docker_stats_YYYYMMDD_HHMMSS.log  # Docker 리소스 사용량 로그
```

---

## 📊 결과 분석

### 1. 리소스 메트릭 확인

```bash
# 리소스 메트릭 CSV 확인
cat batch-logs/batch.log | grep "RESOURCE_METRICS_CSV"
```

**예상 출력:**
```
RESOURCE_METRICS_CSV,crawlPostJob,892,28.5,650,65.0,1,15
```

**해석:**
- Duration: 892초 (약 15분)
- CPU 평균: 28.5%
- 메모리 피크: 650MB
- 메모리 사용률: 65.0%

### 2. Data Skew 분석

```bash
# 회사별 처리 시간 확인 (상위 5개)
cat batch-logs/batch.log | grep "SKEW_CSV" | sort -t',' -k2 -nr | head -5
```

**예상 출력:**
```
SKEW_CSV,Company_A,120,15
SKEW_CSV,Company_B,80,10
SKEW_CSV,Company_C,60,8
SKEW_CSV,Company_D,50,6
SKEW_CSV,Company_E,45,5
```

**해석:**
- Company_A가 120초 소요 (전체의 13%)
- 상위 5개 회사가 전체 시간의 60% 차지 → **심각한 Data Skew**

### 3. 비용 분석

```bash
# 비용 분석 확인
cat batch-logs/batch.log | grep "COST_CSV"
```

**예상 출력:**
```
COST_CSV,crawlPostJob,120,0.00125,0.0000104,0.002,0.0000167
```

**해석:**
- 총 120개 포스트 처리
- 인프라 비용: $0.00125
- 포스트당 인프라 비용: $0.0000104
- API 비용: $0.002
- 포스트당 총 비용: $0.0000167

### 4. 상세 리포트

```bash
# 전체 리포트 확인
cat batch-logs/batch.log | grep -A 30 "RESOURCE METRICS - Job Completed"
```

**예상 출력:**
```
========================================
📊 RESOURCE METRICS - Job Completed
========================================
Job: crawlPostJob
Duration: 892s

🖥️  CPU Metrics:
- Average CPU Load: 28.5%
- Efficiency: 🟡 UNDER-UTILIZED
- Available Cores: 1
- Interpretation: I/O bound or waiting on external APIs

💾 Memory Metrics:
- Peak Memory Used: 650 MB
- Average Memory: 580 MB
- Memory Utilization: 65.0%
- Status: ✅ Within limits

🧵 Thread Metrics:
- Average WAITING threads: 12
- Average RUNNABLE threads: 3
- Interpretation: 🔴 Threads mostly WAITING → I/O bottleneck

📈 Skew Analysis:
- Skewness: 2.8 (🔴 HIGH SKEW)
- Max/Median Ratio: 15.0x
- P95 Time: 90s
- Max Time: 120s
- Interpretation: 🔴 One slow company dominates execution time

💰 Cost Analysis:
- Total Posts Processed: 120
- Infrastructure Cost: $0.00125
- API Cost: $0.002
- Cost per Post: $0.0000167
```

### 5. Docker Stats 분석

Python이 설치된 경우 자동으로 분석됩니다:

```
📈 Docker Stats 분석

CPU 사용률:
  평균: 28.45%
  최대: 45.20%
  최소: 12.30%

메모리 사용률:
  평균: 62.50%
  최대: 68.90%
```

---

## 🔍 추가 분석 도구

### GC 로그 분석

```bash
# GC 통계 확인
cat batch-logs/gc.log | grep "Full GC" | wc -l
cat batch-logs/gc.log | grep "Young GC" | wc -l
```

### Docker Stats 수동 분석

```bash
# CSV 형식으로 변환
cat batch-reports/docker_stats_*.log | awk -F',' '{print $2","$3","$4","$5}' > stats.csv

# Excel이나 Google Sheets로 열어서 그래프 생성
```

---

## 🎯 결과 해석 가이드

### CPU 사용률 해석

| CPU % | 상태 | 의미 | 조치 |
|-------|------|------|------|
| < 20% | 🔴 IDLE | I/O bound, 외부 API 대기 | 병렬 처리, 비동기 호출 |
| 20-50% | 🟡 UNDER-UTILIZED | 일부 I/O 대기 | 동시성 증가 고려 |
| 50-80% | 🟢 OPTIMAL | 정상 | 현재 상태 유지 |
| > 80% | 🔴 SATURATED | CPU 병목 | 알고리즘 최적화 필요 |

### Data Skew 해석

| Skewness | Max/Median | 상태 | 의미 |
|----------|-----------|------|------|
| < 1 | < 3x | 🟢 LOW | 균등 분산 |
| 1-2 | 3-10x | 🟡 MODERATE | 일부 불균형 |
| > 2 | > 10x | 🔴 HIGH | 심각한 불균형, 개선 필요 |

### 스레드 상태 해석

| WAITING / RUNNABLE 비율 | 의미 | 조치 |
|-------------------------|------|------|
| > 4:1 | I/O 병목 | 병렬 처리, connection pool 증가 |
| 2-4:1 | 정상 (일부 대기) | 동시성 미세 조정 |
| < 2:1 | CPU 집약적 | 알고리즘 최적화 |

---

## 🚨 문제 해결

### 1. Docker 빌드 실패

**증상:**
```
ERROR: failed to solve: process "/bin/sh -c ./gradlew :batch:build --no-daemon -x test" did not complete successfully
```

**해결:**
```bash
# 로컬에서 먼저 빌드
./gradlew :batch:build

# 그 다음 Docker 이미지 빌드
docker-compose -f docker-compose.test.yml build
```

### 2. 컨테이너가 즉시 종료됨

**증상:**
```
batch-production-parity exited with code 1
```

**해결:**
```bash
# 로그 확인
docker logs batch-production-parity

# 일반적인 원인: DB 연결 실패
# → .env.test 파일의 DB_HOST, DB_PASSWORD 확인
```

### 3. host.docker.internal 접속 실패 (Linux)

**증상:**
```
Connection refused to host.docker.internal:5432
```

**해결 (Linux):**
```bash
# .env.test 파일 수정
DB_HOST=172.17.0.1  # Docker 기본 게이트웨이
```

### 4. 메모리 부족 (OOMKilled)

**증상:**
```
Container was OOMKilled (exit code 137)
```

**해결:**
```bash
# docker-compose.test.yml에서 메모리 제한 완화 (테스트용)
memory: 2G  # 1G → 2G로 임시 증가

# 또는 JVM 힙 크기 축소
JAVA_OPTS=-Xmx384m -Xms256m  # 512m → 384m
```

---

## 💡 Best Practices

### 1. 프로덕션 DB 직접 접속 시 주의사항

```bash
# ❌ 잘못된 방법: 쓰기 마스터에 직접 접속
DB_HOST=production-master.rds.amazonaws.com

# ✅ 올바른 방법: 읽기 복제본 사용
DB_HOST=production-readonly.rds.amazonaws.com

# 또는: 로컬 DB에 프로덕션 데이터 복사
pg_dump -h production.rds.amazonaws.com -U postgres -d techinsights > prod_dump.sql
psql -U postgres -d techinsights_local < prod_dump.sql
```

### 2. 반복 테스트 시 시간 절약

```bash
# 빌드된 JAR 재사용
./gradlew :batch:build -x test

# Docker 이미지 캐시 활용
docker-compose -f docker-compose.test.yml build --no-cache  # 필요시에만 사용
```

### 3. CI/CD 파이프라인 통합

```yaml
# .github/workflows/batch-performance-test.yml

name: Batch Performance Test

on:
  pull_request:
    paths:
      - 'batch/**'

jobs:
  performance-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Run Production Parity Test
        run: ./scripts/run-production-parity-test.sh
        env:
          DB_HOST: ${{ secrets.TEST_DB_HOST }}
          DB_PASSWORD: ${{ secrets.TEST_DB_PASSWORD }}

      - name: Upload Results
        uses: actions/upload-artifact@v3
        with:
          name: batch-performance-report
          path: batch-reports/
```

---

## 🎓 더 알아보기

- **상세 가이드**: `scripts/PRODUCTION-PARITY-TESTING.md`
- **고급 메트릭**: `scripts/ADVANCED-METRICS-GUIDE.md`
- **안전한 프로덕션 분석**: `scripts/SAFE-PRODUCTION-ANALYSIS.md`
- **측정 전략**: `scripts/MEASUREMENT-STRATEGY.md`

---

## 📞 문의

문제가 발생하면:
1. `docker logs batch-production-parity` 확인
2. `batch-logs/batch.log` 확인
3. `.env.test` 설정 재확인
4. Docker 재시작: `docker-compose -f docker-compose.test.yml down && docker-compose -f docker-compose.test.yml up`
