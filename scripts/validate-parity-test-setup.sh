#!/bin/bash
# ========================================
# 프로덕션 동등 환경 테스트 사전 검증
# ========================================

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_ROOT="/Users/kitoha/.claude-worktrees/TechInsights-Server/blissful-swartz"
cd "$PROJECT_ROOT"

echo "========================================="
echo "🔍 프로덕션 동등 환경 테스트 사전 검증"
echo "========================================="
echo ""

CHECKS_PASSED=0
CHECKS_TOTAL=8

# Check 1: Docker 설치
echo -n "1. Docker 설치 확인... "
if command -v docker &> /dev/null; then
    echo -e "${GREEN}✅${NC}"
    ((CHECKS_PASSED++))
else
    echo -e "${RED}❌ Docker 미설치${NC}"
fi

# Check 2: Docker Compose 설치
echo -n "2. Docker Compose 설치 확인... "
if command -v docker-compose &> /dev/null; then
    echo -e "${GREEN}✅${NC}"
    ((CHECKS_PASSED++))
else
    echo -e "${RED}❌ Docker Compose 미설치${NC}"
fi

# Check 3: Dockerfile 존재
echo -n "3. Dockerfile 존재 확인... "
if [ -f "batch/Dockerfile" ]; then
    echo -e "${GREEN}✅${NC}"
    ((CHECKS_PASSED++))
else
    echo -e "${RED}❌ batch/Dockerfile 없음${NC}"
fi

# Check 4: docker-compose.test.yml 존재
echo -n "4. docker-compose.test.yml 존재 확인... "
if [ -f "docker-compose.test.yml" ]; then
    echo -e "${GREEN}✅${NC}"
    ((CHECKS_PASSED++))
else
    echo -e "${RED}❌ docker-compose.test.yml 없음${NC}"
fi

# Check 5: .env.test 존재
echo -n "5. .env.test 존재 확인... "
if [ -f ".env.test" ]; then
    echo -e "${GREEN}✅${NC}"
    ((CHECKS_PASSED++))
else
    echo -e "${YELLOW}⚠️  .env.test 없음 (테스트 실행 시 생성됨)${NC}"
fi

# Check 6: 로그 디렉토리
echo -n "6. 로그 디렉토리 확인... "
if [ -d "batch-logs" ] && [ -d "batch-reports" ]; then
    echo -e "${GREEN}✅${NC}"
    ((CHECKS_PASSED++))
else
    echo -e "${YELLOW}⚠️  로그 디렉토리 없음 (자동 생성됨)${NC}"
    mkdir -p batch-logs batch-reports
    ((CHECKS_PASSED++))
fi

# Check 7: Docker 실행 중
echo -n "7. Docker 데몬 실행 확인... "
if docker info &> /dev/null; then
    echo -e "${GREEN}✅${NC}"
    ((CHECKS_PASSED++))
else
    echo -e "${RED}❌ Docker가 실행되지 않음${NC}"
fi

# Check 8: 디스크 공간
echo -n "8. 디스크 공간 확인 (최소 5GB)... "
AVAILABLE_SPACE=$(df -h . | awk 'NR==2 {print $4}' | sed 's/G.*//')
if [ "${AVAILABLE_SPACE%.*}" -ge 5 ] 2>/dev/null; then
    echo -e "${GREEN}✅ (${AVAILABLE_SPACE}GB 사용 가능)${NC}"
    ((CHECKS_PASSED++))
else
    echo -e "${YELLOW}⚠️  디스크 공간 부족 (${AVAILABLE_SPACE}GB)${NC}"
fi

echo ""
echo "========================================="
echo "검증 결과: $CHECKS_PASSED/$CHECKS_TOTAL 통과"
echo "========================================="

if [ $CHECKS_PASSED -eq $CHECKS_TOTAL ]; then
    echo -e "${GREEN}✅ 모든 검증 통과! 테스트 실행 가능합니다.${NC}"
    echo ""
    echo "다음 명령어로 테스트를 실행하세요:"
    echo "  ./scripts/run-production-parity-test.sh"
    exit 0
elif [ $CHECKS_PASSED -ge 6 ]; then
    echo -e "${YELLOW}⚠️  경고 사항이 있지만 테스트 실행 가능합니다.${NC}"
    exit 0
else
    echo -e "${RED}❌ 필수 요구사항이 충족되지 않았습니다.${NC}"
    echo ""
    echo "필요한 조치:"
    [ ! command -v docker &> /dev/null ] && echo "  - Docker 설치: https://docs.docker.com/get-docker/"
    [ ! command -v docker-compose &> /dev/null ] && echo "  - Docker Compose 설치"
    [ ! docker info &> /dev/null ] && echo "  - Docker 데몬 시작"
    exit 1
fi
