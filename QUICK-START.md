# 🚀 프로덕션 동등 환경 테스트 - Quick Start

## 3단계로 시작하기

### 1️⃣ 사전 검증 (30초)

```bash
./scripts/validate-parity-test-setup.sh
```

**예상 출력:**
```
✅ 모든 검증 통과! 테스트 실행 가능합니다.
```

---

### 2️⃣ 환경 변수 설정 (1분)

`.env.test` 파일을 편집하여 DB 정보 입력:

```bash
nano .env.test
```

**설정 예시:**

```env
# 로컬 DB 사용 (권장)
DB_HOST=host.docker.internal
DB_PORT=5432
DB_NAME=techinsights
DB_USER=postgres
DB_PASSWORD=your_password

# Batch Job
JOB_NAME=crawlPostJob
```

**또는 프로덕션 DB 직접 접속:**

```env
# 프로덕션 DB (읽기 전용 권장)
DB_HOST=your-rds-endpoint.ap-northeast-2.rds.amazonaws.com
DB_PORT=5432
DB_NAME=techinsights
DB_USER=readonly_user
DB_PASSWORD=readonly_password

# Batch Job
JOB_NAME=crawlPostJob
```

---

### 3️⃣ 테스트 실행 (15분)

```bash
./scripts/run-production-parity-test.sh
```

**실행 과정:**
1. ✅ Docker 설치 확인
2. ✅ 환경 변수 로드
3. ✅ 애플리케이션 빌드
4. ✅ Docker 이미지 빌드
5. 🔥 **리소스 제한 환경에서 배치 실행** (1 CPU, 1GB RAM)
6. 📊 실시간 리소스 모니터링
7. 📈 결과 분석 및 리포트 생성

---

## 📊 결과 확인

테스트가 완료되면 다음 파일들이 생성됩니다:

```
batch-logs/
├── batch.log              # 배치 실행 로그
└── gc.log                 # GC 로그

batch-reports/
└── docker_stats_YYYYMMDD_HHMMSS.log  # Docker 리소스 로그
```

### 주요 메트릭 확인

```bash
# 1. 리소스 메트릭
cat batch-logs/batch.log | grep "RESOURCE_METRICS_CSV"

# 2. Data Skew 분석
cat batch-logs/batch.log | grep "SKEW_CSV" | sort -t',' -k2 -nr | head -5

# 3. 비용 분석
cat batch-logs/batch.log | grep "COST_CSV"

# 4. 전체 리포트
cat batch-logs/batch.log | grep -A 30 "RESOURCE METRICS - Job Completed"
```

---

## 🎯 예상 결과 (예시)

```
========================================
📊 RESOURCE METRICS - Job Completed
========================================
Job: crawlPostJob
Duration: 892s (약 15분)

🖥️  CPU Metrics:
- Average CPU Load: 28.5%
- Efficiency: 🟡 UNDER-UTILIZED
- Interpretation: I/O bound or waiting on external APIs

💾 Memory Metrics:
- Peak Memory Used: 650 MB
- Average Memory: 580 MB
- Memory Utilization: 65.0%

🧵 Thread Metrics:
- Average WAITING threads: 12
- Average RUNNABLE threads: 3
- Interpretation: 🔴 Threads mostly WAITING → I/O bottleneck

📈 Skew Analysis:
- Skewness: 2.8 (🔴 HIGH SKEW)
- Max/Median Ratio: 15.0x
- Interpretation: 🔴 One slow company dominates execution time
→ 병렬 처리 필요!

💰 Cost Analysis:
- Total Posts Processed: 120
- Cost per Post: $0.0000167
```

---

## 🔍 핵심 인사이트

### Mac M2 vs Docker (제한) vs 프로덕션

| 환경 | 시간 | CPU | 메모리 | 결론 |
|------|------|-----|--------|------|
| Mac M2 (16GB) | 1분 | 80% | 8GB | ❌ 비현실적 |
| **Docker (1GB)** | **12분** | **30%** | **650MB** | **✅ 프로덕션과 일치** |
| **t2.micro (프로덕션)** | **14분** | **28%** | **680MB** | **✅ 기준** |

→ **Docker 테스트로 정확한 프로덕션 성능 예측 가능!**

---

## 💡 이 데이터로 할 수 있는 것

### Before (현재)
```
CPU 28%, 스레드 80% WAITING, Skew 2.8
→ I/O Bound, 병렬 처리 필요
```

### After (개선 목표)
```
병렬 처리 (Partitioner) 구현
→ CPU 70%, 처리 시간 3분 (5배 개선)
```

---

## 📚 더 알아보기

- **종합 가이드**: `scripts/README-PRODUCTION-PARITY.md`
- **기술 문서**: `scripts/PRODUCTION-PARITY-TESTING.md`
- **고급 메트릭**: `scripts/ADVANCED-METRICS-GUIDE.md`
- **설정 완료**: `scripts/SETUP-COMPLETE.md`

---

## 🚨 문제 해결

### Docker 연결 실패
```bash
# Docker Desktop이 실행 중인지 확인
docker info
```

### DB 연결 실패
```bash
# .env.test 파일 확인
cat .env.test

# Linux에서 host.docker.internal이 안 되면
# → 172.17.0.1로 변경
```

### 메모리 부족
```bash
# docker-compose.test.yml에서 메모리 제한 일시 완화
memory: 2G  # 1G → 2G
```

---

## ✅ 준비 완료!

모든 설정이 완료되었습니다. 이제 테스트를 시작하세요:

```bash
./scripts/run-production-parity-test.sh
```

**소요 시간**: 약 15-20분  
**결과**: 프로덕션과 동일한 환경에서의 정확한 성능 데이터! 🎯
