#!/bin/bash
# ========================================
# 프로덕션 동등 환경 테스트 실행 스크립트
# ========================================
# t2.micro 스펙으로 제한된 Docker 환경에서 배치를 실행하고
# 리소스 사용량을 측정합니다.
#
# 사용법:
#   ./scripts/run-production-parity-test.sh
# ========================================

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

PROJECT_ROOT="/Users/kitoha/.claude-worktrees/TechInsights-Server/blissful-swartz"
cd "$PROJECT_ROOT"

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}🔬 프로덕션 동등 환경 테스트${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""
echo "목표: t2.micro (1 vCPU, 1GB RAM) 환경에서 배치 성능 측정"
echo ""

# ========================================
# Step 1: 사전 준비
# ========================================
echo -e "${CYAN}[Step 1/6] 사전 준비${NC}"

# Docker 설치 확인
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker가 설치되지 않았습니다.${NC}"
    echo "설치: https://docs.docker.com/get-docker/"
    exit 1
fi
echo -e "${GREEN}✅ Docker 설치 확인${NC}"

# Docker Compose 확인
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose가 설치되지 않았습니다.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker Compose 설치 확인${NC}"

# 로그 디렉토리 생성
mkdir -p batch-logs batch-reports
echo -e "${GREEN}✅ 로그 디렉토리 생성${NC}"
echo ""

# ========================================
# Step 2: 환경 변수 설정
# ========================================
echo -e "${CYAN}[Step 2/6] 환경 변수 설정${NC}"

# .env 파일 확인
if [ ! -f ".env.test" ]; then
    echo -e "${YELLOW}⚠️  .env.test 파일이 없습니다. 생성합니다...${NC}"
    cat > .env.test << 'EOF'
# Database (프로덕션 DB 또는 로컬 DB)
DB_HOST=host.docker.internal
DB_PORT=5432
DB_NAME=techinsights
DB_USER=postgres
DB_PASSWORD=your_password

# Batch Job
JOB_NAME=crawlPostJob
EOF
    echo -e "${YELLOW}📝 .env.test 파일을 수정하여 DB 정보를 입력하세요.${NC}"
    echo ""
    read -p "계속하시겠습니까? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# 환경 변수 로드
export $(cat .env.test | xargs)
echo -e "${GREEN}✅ 환경 변수 로드 완료${NC}"
echo "   DB_HOST: $DB_HOST"
echo "   JOB_NAME: $JOB_NAME"
echo ""

# ========================================
# Step 3: 애플리케이션 빌드
# ========================================
echo -e "${CYAN}[Step 3/6] 애플리케이션 빌드${NC}"

if [ ! -f "batch/build/libs/batch.jar" ]; then
    echo "Gradle 빌드 시작..."
    ./gradlew :batch:build -x test
    echo -e "${GREEN}✅ 빌드 완료${NC}"
else
    echo -e "${GREEN}✅ 이미 빌드됨 (batch/build/libs/batch.jar)${NC}"
fi
echo ""

# ========================================
# Step 4: Docker 이미지 빌드
# ========================================
echo -e "${CYAN}[Step 4/6] Docker 이미지 빌드${NC}"

docker-compose -f docker-compose.test.yml build
echo -e "${GREEN}✅ Docker 이미지 빌드 완료${NC}"
echo ""

# ========================================
# Step 5: 배치 실행 및 모니터링
# ========================================
echo -e "${CYAN}[Step 5/6] 배치 실행 (리소스 제한: 1 CPU, 1GB RAM)${NC}"
echo ""
echo -e "${YELLOW}🔥 프로덕션 t2.micro와 동일한 스펙으로 실행합니다...${NC}"
echo ""

# 백그라운드에서 docker stats 모니터링
STATS_LOG="batch-reports/docker_stats_$(date +%Y%m%d_%H%M%S).log"
echo "타임스탬프,CPU %,메모리 사용,메모리 제한,메모리 %,네트워크 I/O" > "$STATS_LOG"

docker-compose -f docker-compose.test.yml up -d

# 컨테이너 시작 대기
echo "컨테이너 시작 대기 중..."
sleep 5

# docker stats 모니터링 시작 (백그라운드)
{
    while docker ps | grep -q batch-production-parity; do
        docker stats --no-stream --format "{{.Container}},{{.CPUPerc}},{{.MemUsage}},{{.MemPerc}},{{.NetIO}}" batch-production-parity 2>/dev/null | \
        awk -v ts="$(date +%s)" '{print ts","$0}' >> "$STATS_LOG"
        sleep 2
    done
} &
STATS_PID=$!

# 실시간 로그 출력
echo ""
echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}📊 실시간 로그 (Ctrl+C로 중단 가능)${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""

docker logs -f batch-production-parity &
LOGS_PID=$!

# 컨테이너 종료 대기
docker wait batch-production-parity > /dev/null 2>&1

# 백그라운드 프로세스 종료
kill $STATS_PID 2>/dev/null || true
kill $LOGS_PID 2>/dev/null || true

echo ""
echo -e "${GREEN}✅ 배치 실행 완료${NC}"
echo ""

# ========================================
# Step 6: 결과 분석
# ========================================
echo -e "${CYAN}[Step 6/6] 결과 분석${NC}"
echo ""

# 로그 파일 복사
docker cp batch-production-parity:/app/logs/batch.log batch-logs/ 2>/dev/null || true
docker cp batch-production-parity:/app/logs/gc.log batch-logs/ 2>/dev/null || true

# 컨테이너 정리
docker-compose -f docker-compose.test.yml down

# 리소스 메트릭 추출
echo -e "${YELLOW}📊 리소스 사용량 분석${NC}"
echo ""

if [ -f "batch-logs/batch.log" ]; then
    # CPU/메모리 메트릭
    echo "=== 리소스 메트릭 ==="
    grep "RESOURCE_METRICS_CSV" batch-logs/batch.log | tail -1

    # Data Skew
    echo ""
    echo "=== 회사별 처리 시간 (상위 5개) ==="
    grep "SKEW_CSV" batch-logs/batch.log | sort -t',' -k2 -nr | head -5

    # 비용
    echo ""
    echo "=== 비용 분석 ==="
    grep "COST_CSV" batch-logs/batch.log | tail -1

    # 전체 리포트
    echo ""
    echo "=== 상세 리포트 ==="
    grep -A 30 "RESOURCE METRICS - Job Completed" batch-logs/batch.log | head -35
fi

# Docker Stats 분석
if [ -f "$STATS_LOG" ]; then
    echo ""
    echo -e "${YELLOW}📈 Docker Stats 분석${NC}"
    echo ""

    # Python이 설치되어 있으면 분석
    if command -v python3 &> /dev/null; then
        python3 << 'PYTHON_SCRIPT'
import sys
import csv

try:
    with open('$STATS_LOG', 'r') as f:
        reader = csv.DictReader(f)
        cpu_values = []
        mem_values = []

        for row in reader:
            try:
                cpu = float(row['CPU %'].replace('%', ''))
                mem = float(row['메모리 %'].replace('%', ''))
                cpu_values.append(cpu)
                mem_values.append(mem)
            except:
                pass

    if cpu_values and mem_values:
        print(f"CPU 사용률:")
        print(f"  평균: {sum(cpu_values)/len(cpu_values):.2f}%")
        print(f"  최대: {max(cpu_values):.2f}%")
        print(f"  최소: {min(cpu_values):.2f}%")
        print()
        print(f"메모리 사용률:")
        print(f"  평균: {sum(mem_values)/len(mem_values):.2f}%")
        print(f"  최대: {max(mem_values):.2f}%")
    else:
        print("데이터가 충분하지 않습니다.")
except Exception as e:
    print(f"분석 중 오류: {e}")
PYTHON_SCRIPT
    else
        echo "Python이 설치되지 않아 자동 분석을 건너뜁니다."
        echo "수동 확인: cat $STATS_LOG"
    fi
fi

# ========================================
# 완료
# ========================================
echo ""
echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}✅ 테스트 완료!${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""
echo "생성된 파일:"
echo "  - 배치 로그: batch-logs/batch.log"
echo "  - GC 로그: batch-logs/gc.log"
echo "  - Docker Stats: $STATS_LOG"
echo ""
echo "다음 단계:"
echo "  1. 로그 확인: cat batch-logs/batch.log | grep 'RESOURCE_METRICS'"
echo "  2. Skew 분석: cat batch-logs/batch.log | grep 'SKEW_CSV'"
echo "  3. 비용 분석: cat batch-logs/batch.log | grep 'COST_CSV'"
echo ""
echo "프로덕션 비교:"
echo "  - 이 테스트 결과와 프로덕션 실행 결과를 비교하세요."
echo "  - CPU 사용률, 처리 시간, 메모리 사용량이 유사해야 합니다."
echo ""
