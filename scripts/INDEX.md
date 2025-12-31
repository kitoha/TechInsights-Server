# 📚 Batch 성능 측정 및 개선 - 전체 문서 인덱스

## 🚀 빠른 시작

**처음 시작하시나요?** → [`/QUICK-START.md`](../QUICK-START.md)

3단계로 프로덕션 동등 환경 테스트를 시작하세요.

---

## 📖 문서 구조

### 1️⃣ 시작 가이드

| 문서 | 설명 | 대상 |
|------|------|------|
| **[QUICK-START.md](../QUICK-START.md)** | 3단계 빠른 시작 가이드 | 모든 사용자 ⭐ |
| **[SETUP-COMPLETE.md](SETUP-COMPLETE.md)** | 설정 완료 확인 및 다음 단계 | 모든 사용자 |

### 2️⃣ 프로덕션 동등 환경 테스팅

| 문서 | 설명 | 대상 |
|------|------|------|
| **[README-PRODUCTION-PARITY.md](README-PRODUCTION-PARITY.md)** | 종합 사용자 가이드 | 사용자 ⭐⭐⭐ |
| **[PRODUCTION-PARITY-TESTING.md](PRODUCTION-PARITY-TESTING.md)** | 상세 기술 문서 | 개발자 |

**핵심 개념:**
- Docker로 t2.micro 스펙 시뮬레이션
- Mac M2에서도 프로덕션과 동일한 측정 결과
- 리소스 제한 (1 CPU, 1GB RAM)

### 3️⃣ 측정 전략 및 데이터 수집

| 문서 | 설명 | 대상 |
|------|------|------|
| **[MEASUREMENT-STRATEGY.md](MEASUREMENT-STRATEGY.md)** | 데이터 유효성 및 측정 전략 | 모든 사용자 ⭐ |
| **[SAFE-PRODUCTION-ANALYSIS.md](SAFE-PRODUCTION-ANALYSIS.md)** | t2.micro에서 안전한 데이터 수집 | DevOps |
| **[batch-baseline-analysis.md](batch-baseline-analysis.md)** | Baseline 데이터 수집 방법 | 개발자 |

**핵심 개념:**
- 통계적 유효성: 최소 7회 실행 필요
- 로컬에서 프로덕션 DB 조회 (서버 부하 0)
- 실행 기록 없을 때 코드 정적 분석

### 4️⃣ 고급 메트릭

| 문서 | 설명 | 대상 |
|------|------|------|
| **[ADVANCED-METRICS-GUIDE.md](ADVANCED-METRICS-GUIDE.md)** | BigTech 수준 메트릭 구현 | 개발자 ⭐⭐⭐ |

**포함된 메트릭:**
- **컴퓨팅 효율성**: CPU 사용률 vs 처리 시간
- **Data Skew**: 회사별 처리 시간 분포 (Skewness, P95, Max/Median)
- **비용 분석**: 포스트당 인프라 + API 비용

### 5️⃣ SQL 쿼리 및 스크립트

| 파일 | 설명 | 용도 |
|------|------|------|
| **export_batch_metadata.sql** | Spring Batch 메타데이터 분석 쿼리 (8개) | 실행 통계, 실패 패턴 |
| **analyze_company_performance.sql** | 회사별 성능 분석 쿼리 (10개) | Data Skew, 데이터 신선도 |
| **quick_baseline_check.sh** | 5초 빠른 헬스체크 | 전체 시스템 상태 |
| **analyze_code_issues.sh** | 코드 정적 분석 | 실행 기록 없을 때 |
| **run-production-parity-test.sh** | 프로덕션 동등 환경 테스트 실행 | 메인 테스트 스크립트 ⭐ |
| **validate-parity-test-setup.sh** | 테스트 사전 검증 | 테스트 실행 전 확인 |

---

## 🎯 사용 시나리오별 가이드

### 시나리오 1: "처음 시작합니다"

1. [`QUICK-START.md`](../QUICK-START.md) 읽기
2. `.env.test` 파일 설정
3. `./scripts/run-production-parity-test.sh` 실행

**소요 시간**: 20분

---

### 시나리오 2: "현재 배치 성능을 측정하고 싶어요"

**프로덕션 실행 기록이 있는 경우:**

```bash
# 1. 프로덕션 DB에서 메타데이터 수집 (로컬에서 실행)
psql -h your-rds-endpoint.com -U postgres -d techinsights -f scripts/export_batch_metadata.sql

# 2. 회사별 성능 분석
psql -h your-rds-endpoint.com -U postgres -d techinsights -f scripts/analyze_company_performance.sql
```

**프로덕션 실행 기록이 없는 경우:**

```bash
# 코드 정적 분석
./scripts/analyze_code_issues.sh

# Docker로 프로덕션 동등 환경 테스트
./scripts/run-production-parity-test.sh
```

**참고 문서**: [`MEASUREMENT-STRATEGY.md`](MEASUREMENT-STRATEGY.md)

---

### 시나리오 3: "t2.micro 프로덕션 서버에서 데이터를 수집하고 싶어요"

**중요**: t2.micro는 메모리가 1GB밖에 없어서 직접 쿼리 실행 시 OOM 위험!

**올바른 방법:**

```bash
# 로컬 머신에서 프로덕션 DB에 접속
psql -h production-rds-endpoint.com -U postgres -d techinsights -f scripts/export_batch_metadata.sql > results.txt
```

**참고 문서**: [`SAFE-PRODUCTION-ANALYSIS.md`](SAFE-PRODUCTION-ANALYSIS.md)

---

### 시나리오 4: "BigTech 수준의 메트릭을 추가하고 싶어요"

1. [`ADVANCED-METRICS-GUIDE.md`](ADVANCED-METRICS-GUIDE.md) 읽기
2. Listener 클래스 구현:
   - `ResourceMetricsListener.kt` (CPU, 메모리, 스레드)
   - `SkewAnalysisProcessor.kt` (회사별 처리 시간)
   - `CostAnalysisListener.kt` (비용 분석)
3. Spring Batch 설정에 추가
4. 테스트 실행

**소요 시간**: 2-3시간

---

### 시나리오 5: "로컬 Mac에서 측정한 결과가 프로덕션과 달라요"

**문제**: Mac M2 (8 코어, 16GB) vs t2.micro (1 코어, 1GB)

**해결**:

```bash
# Docker로 리소스 제한하여 프로덕션과 동일한 환경 구성
./scripts/run-production-parity-test.sh
```

**결과**:
- Mac M2: 1분, CPU 80% ❌
- Docker (제한): 12분, CPU 30% ✅ (프로덕션과 일치)
- t2.micro: 14분, CPU 28% ✅

**참고 문서**: [`PRODUCTION-PARITY-TESTING.md`](PRODUCTION-PARITY-TESTING.md)

---

### 시나리오 6: "개선 전후 비교를 하고 싶어요"

```bash
# Before: 현재 상태 측정
./scripts/run-production-parity-test.sh
cp batch-logs/batch.log baseline_before.log

# 개선 사항 구현 (예: 병렬 처리 Partitioner)

# After: 개선 후 측정
./scripts/run-production-parity-test.sh
cp batch-logs/batch.log baseline_after.log

# 비교
diff baseline_before.log baseline_after.log | grep "RESOURCE_METRICS"
```

---

## 📊 메트릭 해석 가이드

### CPU 사용률 해석

| CPU % | 의미 | 조치 |
|-------|------|------|
| < 20% | 🔴 I/O Bound | 병렬 처리, 비동기 호출 |
| 20-50% | 🟡 UNDER-UTILIZED | 동시성 증가 |
| 50-80% | 🟢 OPTIMAL | 현재 상태 유지 |
| > 80% | 🔴 CPU SATURATED | 알고리즘 최적화 |

### Data Skew 해석

| Skewness | Max/Median | 의미 |
|----------|-----------|------|
| < 1 | < 3x | 🟢 균등 분산 |
| 1-2 | 3-10x | 🟡 일부 불균형 |
| > 2 | > 10x | 🔴 심각한 불균형 |

### 스레드 상태 해석

| WAITING/RUNNABLE | 의미 |
|------------------|------|
| > 4:1 | 🔴 I/O 병목 |
| 2-4:1 | 🟡 정상 (일부 대기) |
| < 2:1 | 🟢 CPU 집약적 |

**자세한 해석**: [`README-PRODUCTION-PARITY.md`](README-PRODUCTION-PARITY.md) > "결과 해석 가이드"

---

## 🔧 도구 및 스크립트

### 실행 스크립트

```bash
# 프로덕션 동등 환경 테스트 (메인)
./scripts/run-production-parity-test.sh

# 사전 검증
./scripts/validate-parity-test-setup.sh

# 빠른 헬스체크
./scripts/quick_baseline_check.sh

# 코드 정적 분석
./scripts/analyze_code_issues.sh
```

### SQL 쿼리

```bash
# Spring Batch 메타데이터
psql -f scripts/export_batch_metadata.sql

# 회사별 성능
psql -f scripts/analyze_company_performance.sql
```

---

## 🎓 학습 경로

### Level 1: 기초 (30분)
1. `QUICK-START.md` 읽기
2. 테스트 1회 실행
3. 결과 확인

### Level 2: 중급 (2시간)
1. `README-PRODUCTION-PARITY.md` 읽기
2. `MEASUREMENT-STRATEGY.md` 이해
3. SQL 쿼리로 데이터 수집
4. 개선 전후 비교

### Level 3: 고급 (1일)
1. `ADVANCED-METRICS-GUIDE.md` 구현
2. `PRODUCTION-PARITY-TESTING.md` 모든 방법 시도
3. CI/CD 파이프라인 통합
4. Chaos Engineering 적용

---

## 🆘 문제 해결

### 자주 묻는 질문

**Q: Docker 빌드가 실패해요**
```bash
# 로컬에서 먼저 빌드
./gradlew :batch:build
docker-compose -f docker-compose.test.yml build
```

**Q: host.docker.internal이 안 돼요 (Linux)**
```bash
# .env.test에서 DB_HOST 변경
DB_HOST=172.17.0.1
```

**Q: 메모리 부족으로 죽어요 (OOMKilled)**
```bash
# docker-compose.test.yml에서 메모리 제한 완화
memory: 2G  # 1G → 2G
```

**더 많은 문제 해결**: [`README-PRODUCTION-PARITY.md`](README-PRODUCTION-PARITY.md) > "문제 해결"

---

## 📞 추가 도움

문제가 발생하면:
1. 해당 문서의 "문제 해결" 섹션 확인
2. `docker logs batch-production-parity` 확인
3. `batch-logs/batch.log` 확인
4. GitHub Issue 생성

---

## 🗂️ 파일 구조

```
TechInsights-Server/
├── QUICK-START.md                          # ⭐ 시작 가이드
├── .env.test                                # 환경 변수
├── docker-compose.test.yml                  # Docker 설정
├── batch/
│   ├── Dockerfile                           # 배치 이미지
│   └── build/libs/batch.jar
├── batch-logs/                              # 로그 출력
│   ├── batch.log
│   └── gc.log
├── batch-reports/                           # 리포트 출력
│   └── docker_stats_*.log
└── scripts/
    ├── INDEX.md                             # ⭐ 이 파일
    ├── QUICK-START.md (심볼릭 링크)
    ├── SETUP-COMPLETE.md
    ├── README-PRODUCTION-PARITY.md          # ⭐ 종합 가이드
    ├── PRODUCTION-PARITY-TESTING.md
    ├── ADVANCED-METRICS-GUIDE.md            # ⭐ 고급 메트릭
    ├── SAFE-PRODUCTION-ANALYSIS.md
    ├── MEASUREMENT-STRATEGY.md
    ├── batch-baseline-analysis.md
    ├── export_batch_metadata.sql
    ├── analyze_company_performance.sql
    ├── run-production-parity-test.sh        # ⭐ 메인 스크립트
    ├── validate-parity-test-setup.sh
    ├── quick_baseline_check.sh
    └── analyze_code_issues.sh
```

---

## ✅ 다음 단계

1. **지금 바로 시작**: [`QUICK-START.md`](../QUICK-START.md)
2. **상세 가이드 읽기**: [`README-PRODUCTION-PARITY.md`](README-PRODUCTION-PARITY.md)
3. **고급 메트릭 구현**: [`ADVANCED-METRICS-GUIDE.md`](ADVANCED-METRICS-GUIDE.md)
4. **프로덕션 배포**: CI/CD 파이프라인 통합

---

**마지막 업데이트**: 2025-12-31
**작성자**: Claude Code
**버전**: 1.0.0
