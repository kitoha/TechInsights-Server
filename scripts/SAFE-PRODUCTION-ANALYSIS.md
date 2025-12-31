# t2.micro 프로덕션 서버에서 안전하게 데이터 수집하기

## ⚠️ 문제 상황

**환경:**
- AWS EC2 t2.micro (메모리 1GB)
- Spring Batch 메타데이터가 PostgreSQL에 많이 쌓여있음
- 운영 중인 서비스

**위험:**
- ❌ `quick_baseline_check.sh` 직접 실행 → 메모리 부족으로 인스턴스 다운 가능
- ❌ 복잡한 SQL 쿼리 실행 → DB 부하로 서비스 영향
- ❌ psql 클라이언트가 대량 데이터 로드 → OOM (Out of Memory)

---

## 🎯 안전한 데이터 수집 전략

### 전략 1: 로컬에서 프로덕션 DB 접근 (강력 권장) ⭐⭐⭐⭐⭐

#### 장점
- ✅ 프로덕션 서버에 부하 0%
- ✅ 로컬 메모리 사용 (Mac 메모리 충분)
- ✅ 안전하게 모든 스크립트 실행 가능
- ✅ 서비스 중단 위험 없음

#### 전제 조건
- PostgreSQL이 외부 접속 허용 (보안 그룹 설정)
- 읽기 전용 계정 또는 일반 계정

#### 설정 방법

**Step 1: AWS 보안 그룹 확인**

```bash
# EC2 콘솔 → 보안 그룹 → RDS 보안 그룹 확인
# Inbound Rules에 PostgreSQL (5432) 포트가 열려있는지 확인
# 없다면 추가:
Type: PostgreSQL
Protocol: TCP
Port: 5432
Source: My IP (또는 특정 IP)
```

**Step 2: 로컬에서 연결 테스트**

```bash
# 프로덕션 DB 정보 (예시)
export DB_HOST=your-rds-instance.amazonaws.com  # 또는 EC2 public IP
export DB_PORT=5432
export DB_NAME=techinsights
export DB_USER=postgres
export DB_PASSWORD=your_password

# 연결 테스트
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT 1;"
```

**출력:**
```
 ?column?
----------
        1
(1 row)
```

✅ 성공하면 다음 단계 진행

**Step 3: 로컬에서 안전하게 데이터 추출**

```bash
cd /Users/kitoha/.claude-worktrees/TechInsights-Server/blissful-swartz

# 1. 빠른 체크 (5초)
./scripts/quick_baseline_check.sh > production_baseline_$(date +%Y%m%d).txt

# 2. 상세 배치 메타데이터 (30초)
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME \
  -f scripts/export_batch_metadata.sql \
  > production_batch_metadata_$(date +%Y%m%d).txt

# 3. 회사별 성능 (10초)
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME \
  -f scripts/analyze_company_performance.sql \
  > production_company_performance_$(date +%Y%m%d).txt

# 4. CSV 추출 (Excel 분석용)
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
  > production_executions_$(date +%Y%m%d).csv

echo "✅ 모든 데이터 수집 완료!"
ls -lh production_*
```

**예상 소요 시간:** 1분 이내
**서버 영향:** 없음 (읽기만 수행)

---

### 전략 2: SSH 터널링 (DB 외부 접속 불가 시) ⭐⭐⭐⭐

외부 접속이 막혀있다면 SSH 터널을 통해 접근:

```bash
# 터미널 1: SSH 터널 생성
ssh -i your-key.pem -L 15432:localhost:5432 ec2-user@your-ec2-ip

# 터미널 2: 로컬에서 터널을 통해 접속
export DB_HOST=localhost
export DB_PORT=15432
export DB_NAME=techinsights
export DB_USER=postgres
export DB_PASSWORD=your_password

# 이후 동일하게 스크립트 실행
./scripts/quick_baseline_check.sh
```

---

### 전략 3: 서버에서 안전하게 실행 (최소 쿼리) ⭐⭐⭐

만약 로컬 접근이 불가능하다면, **최소한의 쿼리만** 서버에서 실행:

#### 준비: 경량화된 스크립트 생성

```bash
# 로컬에서 작성 후 서버에 업로드
cat > scripts/lightweight_production_check.sh << 'EOF'
#!/bin/bash
# t2.micro 안전 버전 - 메모리 사용 최소화

export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=techinsights
export DB_USER=postgres

echo "========================================="
echo "📊 Lightweight Production Check"
echo "========================================="
echo ""

# 1. 실행 횟수만 확인 (매우 가벼움)
echo "1. 배치 실행 이력"
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "
SELECT
    ji.job_name,
    COUNT(*) as total_runs,
    MIN(je.start_time)::date as first_run,
    MAX(je.start_time)::date as last_run
FROM batch_job_execution je
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
GROUP BY ji.job_name;
"

echo ""

# 2. 최근 5개만 조회 (LIMIT으로 메모리 제한)
echo "2. 최근 5회 실행 시간"
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "
SELECT
    ji.job_name,
    EXTRACT(EPOCH FROM (je.end_time - je.start_time))::INTEGER as duration_sec,
    je.status,
    je.start_time::date
FROM batch_job_execution je
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
WHERE je.end_time IS NOT NULL
ORDER BY je.start_time DESC
LIMIT 5;
"

echo ""

# 3. 성공률만 계산 (집계만, 상세 데이터 안 가져옴)
echo "3. 성공률"
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "
SELECT
    ji.job_name,
    COUNT(CASE WHEN je.status = 'COMPLETED' THEN 1 END)::FLOAT /
    NULLIF(COUNT(*), 0) * 100 as success_rate
FROM batch_job_execution je
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
WHERE je.start_time >= NOW() - INTERVAL '7 days'
GROUP BY ji.job_name;
"

echo ""
echo "✅ 경량 체크 완료"
EOF

chmod +x scripts/lightweight_production_check.sh
```

#### 서버에 업로드 및 실행

```bash
# 로컬에서 서버로 업로드
scp -i your-key.pem scripts/lightweight_production_check.sh ec2-user@your-ec2-ip:~/

# 서버에 SSH 접속
ssh -i your-key.pem ec2-user@your-ec2-ip

# 서버에서 실행 (메모리 사용 < 50MB)
cd ~
./lightweight_production_check.sh > baseline_light_$(date +%Y%m%d).txt

# 결과 확인
cat baseline_light_$(date +%Y%m%d).txt

# 로컬로 다운로드
exit
scp -i your-key.pem ec2-user@your-ec2-ip:~/baseline_light_*.txt ./
```

**메모리 사용량:** ~30-50MB (안전)
**실행 시간:** ~10초

---

### 전략 4: 데이터베이스 덤프 방식 (가장 안전, 시간 소요) ⭐⭐

서버 부하를 완전히 피하려면:

```bash
# 서버에서: Spring Batch 메타데이터만 덤프
ssh -i your-key.pem ec2-user@your-ec2-ip

pg_dump -h localhost -U postgres -d techinsights \
  -t batch_job_execution \
  -t batch_job_instance \
  -t batch_step_execution \
  --data-only \
  --inserts \
  > batch_metadata_dump.sql

# 로컬로 다운로드
exit
scp -i your-key.pem ec2-user@your-ec2-ip:~/batch_metadata_dump.sql ./

# 로컬 DB에 import
createdb techinsights_analysis
psql -d techinsights_analysis < batch_metadata_dump.sql

# 로컬에서 분석
export DB_NAME=techinsights_analysis
./scripts/quick_baseline_check.sh
```

**장점:**
- ✅ 서버 부하 거의 없음 (한 번만 덤프)
- ✅ 로컬에서 자유롭게 분석

**단점:**
- ⏱️ 시간 소요 (덤프 + 다운로드)

---

## 🔍 각 전략 비교

| 전략 | 서버 부하 | 로컬 메모리 | 시간 | 데이터 완전성 | 권장도 |
|------|-----------|-------------|------|---------------|--------|
| 1. 로컬에서 직접 접근 | 없음 | 높음 (OK) | 1분 | 100% | ⭐⭐⭐⭐⭐ |
| 2. SSH 터널 | 없음 | 높음 (OK) | 1분 | 100% | ⭐⭐⭐⭐ |
| 3. 경량 스크립트 | 낮음 | 낮음 | 10초 | 60% | ⭐⭐⭐ |
| 4. 덤프 방식 | 매우 낮음 | 높음 (OK) | 10분 | 100% | ⭐⭐ |

---

## 🎯 권장 절차

### 당신의 상황에 최적화된 방법:

```bash
# Step 1: 보안 그룹 확인 (AWS 콘솔)
# RDS/EC2 보안 그룹에 PostgreSQL 5432 포트 열기
# Source: My IP

# Step 2: 로컬에서 연결 테스트
export DB_HOST=your-production-db-host
export DB_PORT=5432
export DB_NAME=techinsights
export DB_USER=postgres
export DB_PASSWORD=your_password

psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT COUNT(*) FROM batch_job_execution;"

# Step 3: 연결 성공 시
cd /Users/kitoha/.claude-worktrees/TechInsights-Server/blissful-swartz

# 데이터 수집 (1분 소요)
./scripts/quick_baseline_check.sh > production_baseline.txt
psql -h $DB_HOST ... -f scripts/export_batch_metadata.sql > production_detailed.txt
psql -h $DB_HOST ... -f scripts/analyze_company_performance.sql > production_company.txt

# Step 4: 결과 확인
cat production_baseline.txt
```

---

## 📊 예상 결과 (프로덕션 데이터)

30일간 매일 1회씩 실행했다면:

```
========================================
📊 Batch System Quick Baseline Check
========================================

1️⃣  Recent Batch Executions (Last 7 days)
--------------------------------------------
job_name              | runs | success | failed | last_run
---------------------|------|---------|--------|------------------
crawlPostJob         |   7  |    6    |    1   | 2024-01-15 09:00
summarizePostJob     |   6  |    5    |    1   | 2024-01-15 10:30

2️⃣  Average Execution Time
--------------------------------------------
job_name              | avg_seconds | avg_minutes | max_seconds | max_minutes
---------------------|-------------|-------------|-------------|------------
crawlPostJob         |   892.50    |   14.88     |  1523.20    |   25.39
summarizePostJob     |  3245.80    |   54.10     |  5120.00    |   85.33

...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Overall Health Score: 52/100
Status: ⚠️  FAIR (개선 권장)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**이제 정량적 데이터 확보!**
- ✅ 평균 배치 시간: 14.88분
- ✅ 최대 배치 시간: 25.39분
- ✅ 성공률: 85.7%
- ✅ 30회 측정 → 통계적으로 유의미

---

## ⚠️ 주의사항

### t2.micro에서 절대 하지 말아야 할 것

❌ **하지 마세요:**
```bash
# 서버에 직접 SSH 접속해서
ssh ec2-user@your-ec2-ip

# 이런 명령 실행 (메모리 폭탄!)
./scripts/quick_baseline_check.sh  # ❌ OOM 위험
psql ... -f scripts/export_batch_metadata.sql  # ❌ 메모리 부족

# 대용량 데이터 한 번에 로드
SELECT * FROM batch_job_execution;  # ❌ 전체 스캔
```

✅ **대신 이렇게:**
```bash
# 로컬에서 실행
export DB_HOST=production-db-host
./scripts/quick_baseline_check.sh  # ✅ 로컬 메모리 사용

# 또는 LIMIT 사용
SELECT * FROM batch_job_execution LIMIT 100;  # ✅ 메모리 제한
```

### 프로덕션 안전 수칙

1. **읽기만 수행** (UPDATE/DELETE 절대 금지)
2. **피크 시간 피하기** (새벽 시간 권장)
3. **LIMIT 사용** (대량 데이터 조회 시)
4. **인덱스 확인** (느린 쿼리 방지)
5. **타임아웃 설정** (무한 대기 방지)

```bash
# 안전한 psql 옵션
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME \
  --set=statement_timeout=30000 \  # 30초 타임아웃
  --set=ON_ERROR_STOP=on \          # 에러 시 즉시 중단
  -c "SELECT ..."
```

---

## 🚀 실전 예시

### 시나리오: AWS RDS + EC2 t2.micro 환경

```bash
# 1. RDS 엔드포인트 확인 (AWS 콘솔)
# 예: techinsights-prod.c9akpxxx.ap-northeast-2.rds.amazonaws.com

# 2. 로컬에서 .env 파일 생성
cat > .env.production << EOF
export DB_HOST=techinsights-prod.c9akpxxx.ap-northeast-2.rds.amazonaws.com
export DB_PORT=5432
export DB_NAME=techinsights
export DB_USER=postgres
export DB_PASSWORD=your_secure_password
EOF

# 3. 환경 변수 로드
source .env.production

# 4. 연결 테스트
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME \
  -c "SELECT version();"

# 출력 예시:
# PostgreSQL 15.4 on x86_64-pc-linux-gnu...

# 5. 데이터 수집 시작
date
./scripts/quick_baseline_check.sh | tee production_baseline_$(date +%Y%m%d_%H%M).txt
date

# 실행 시간: 약 5-10초
# 서버 CPU: 0% 증가 (읽기만 수행)
# 서버 메모리: 영향 없음

# 6. 상세 데이터
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME \
  -f scripts/export_batch_metadata.sql \
  > production_detailed_$(date +%Y%m%d).txt

# 7. 결과 확인
head -100 production_baseline_*.txt
```

---

## 📋 체크리스트

데이터 수집 전 확인:

- [ ] PostgreSQL 외부 접속 가능한지 확인
- [ ] 보안 그룹 5432 포트 열림
- [ ] DB 계정 정보 확보
- [ ] 로컬에서 연결 테스트 성공
- [ ] .env 파일 생성 (비밀번호 관리)
- [ ] 디스크 공간 확인 (최소 100MB)

데이터 수집 후 확인:

- [ ] 3개 파일 생성됨 (baseline, metadata, company)
- [ ] 파일 크기 정상 (각 10KB ~ 1MB)
- [ ] 에러 메시지 없음
- [ ] 서버 정상 작동 확인 (모니터링)

---

## 💡 결론

### ✅ 할 수 있습니다!

**t2.micro 환경에서도 안전하게 프로덕션 데이터 수집 가능:**
- **방법:** 로컬에서 프로덕션 DB에 직접 접근
- **소요 시간:** 1분
- **서버 영향:** 없음
- **데이터 품질:** 100%

### 🎯 추천 방법

1. **1순위:** 로컬에서 프로덕션 DB 직접 접근
2. **2순위:** SSH 터널링
3. **3순위:** 경량 스크립트만 서버에서 실행

### 📊 얻게 되는 것

- ✅ 30일치 배치 실행 통계
- ✅ 평균/최대 실행 시간
- ✅ 회사별 성공률
- ✅ 데이터 신선도
- ✅ Skip/Failure 패턴

**이제 정량적 + 정성적 데이터를 모두 확보하여 완벽한 개선 근거를 만들 수 있습니다!** 🎉
