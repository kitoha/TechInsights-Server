#!/bin/bash
# ========================================
# Quick Baseline Check Script
# ========================================
# 빠르게 현재 배치 시스템 상태를 확인하는 스크립트
#
# 사용법:
#   ./scripts/quick_baseline_check.sh
#
# 환경 변수 설정 (옵션):
#   export DB_HOST=localhost
#   export DB_PORT=5432
#   export DB_NAME=techinsights
#   export DB_USER=your_username
#   export DB_PASSWORD=your_password
# ========================================

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 기본 설정
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-techinsights}
DB_USER=${DB_USER:-postgres}
PGPASSWORD=${DB_PASSWORD}

export PGPASSWORD

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}📊 Batch System Quick Baseline Check${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# PostgreSQL 연결 확인
echo -e "${YELLOW}🔌 Checking database connection...${NC}"
if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Database connection successful${NC}"
else
    echo -e "${RED}❌ Database connection failed${NC}"
    echo "Please check your database credentials in environment variables"
    exit 1
fi
echo ""

# 1. 최근 배치 실행 확인
echo -e "${YELLOW}1️⃣  Recent Batch Executions (Last 7 days)${NC}"
echo "--------------------------------------------"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << 'EOF'
SELECT
    ji.job_name,
    COUNT(*) as runs,
    COUNT(CASE WHEN je.status = 'COMPLETED' THEN 1 END) as success,
    COUNT(CASE WHEN je.status = 'FAILED' THEN 1 END) as failed,
    TO_CHAR(MAX(je.start_time), 'YYYY-MM-DD HH24:MI') as last_run
FROM batch_job_execution je
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
WHERE je.start_time >= NOW() - INTERVAL '7 days'
GROUP BY ji.job_name
ORDER BY ji.job_name;
EOF
echo ""

# 2. 평균 실행 시간
echo -e "${YELLOW}2️⃣  Average Execution Time${NC}"
echo "--------------------------------------------"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << 'EOF'
SELECT
    ji.job_name,
    ROUND(AVG(EXTRACT(EPOCH FROM (je.end_time - je.start_time))), 2) as avg_seconds,
    ROUND(AVG(EXTRACT(EPOCH FROM (je.end_time - je.start_time))) / 60, 2) as avg_minutes,
    ROUND(MAX(EXTRACT(EPOCH FROM (je.end_time - je.start_time))), 2) as max_seconds,
    ROUND(MAX(EXTRACT(EPOCH FROM (je.end_time - je.start_time))) / 60, 2) as max_minutes
FROM batch_job_execution je
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
WHERE je.start_time >= NOW() - INTERVAL '7 days'
  AND je.end_time IS NOT NULL
GROUP BY ji.job_name
ORDER BY avg_seconds DESC;
EOF
echo ""

# 3. 처리량 통계
echo -e "${YELLOW}3️⃣  Processing Statistics${NC}"
echo "--------------------------------------------"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << 'EOF'
SELECT
    se.step_name,
    ROUND(AVG(se.read_count), 2) as avg_read,
    ROUND(AVG(se.write_count), 2) as avg_write,
    SUM(se.skip_count) as total_skip,
    ROUND(AVG(se.write_count::NUMERIC / NULLIF(EXTRACT(EPOCH FROM (se.end_time - se.start_time)), 0)), 2) as avg_throughput_per_sec
FROM batch_step_execution se
WHERE se.start_time >= NOW() - INTERVAL '7 days'
GROUP BY se.step_name
ORDER BY se.step_name;
EOF
echo ""

# 4. 회사별 데이터 신선도
echo -e "${YELLOW}4️⃣  Data Freshness by Company${NC}"
echo "--------------------------------------------"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << 'EOF'
SELECT
    c.name,
    COUNT(p.id) as total_posts,
    TO_CHAR(MAX(p.published_at), 'YYYY-MM-DD') as last_post,
    EXTRACT(DAY FROM (NOW() - MAX(p.published_at)))::INTEGER as days_old,
    CASE
        WHEN EXTRACT(DAY FROM (NOW() - MAX(p.published_at))) > 7 THEN '⚠️ '
        WHEN EXTRACT(DAY FROM (NOW() - MAX(p.published_at))) > 3 THEN '⚡'
        ELSE '✅'
    END as status
FROM company c
LEFT JOIN post p ON p.company_id = c.id
GROUP BY c.id, c.name
ORDER BY days_old DESC NULLS LAST;
EOF
echo ""

# 5. 요약/임베딩 진행률
echo -e "${YELLOW}5️⃣  Summary & Embedding Progress${NC}"
echo "--------------------------------------------"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << 'EOF'
SELECT
    COUNT(*) as total_posts,
    COUNT(CASE WHEN is_summary = true THEN 1 END) as summarized,
    COUNT(CASE WHEN is_embedding = true THEN 1 END) as embedded,
    ROUND(COUNT(CASE WHEN is_summary = true THEN 1 END)::NUMERIC / NULLIF(COUNT(*), 0) * 100, 2) as summary_pct,
    ROUND(COUNT(CASE WHEN is_embedding = true THEN 1 END)::NUMERIC / NULLIF(COUNT(*), 0) * 100, 2) as embedding_pct
FROM post;
EOF
echo ""

# 6. 최근 실패 확인
echo -e "${YELLOW}6️⃣  Recent Failures (Last 7 days)${NC}"
echo "--------------------------------------------"
FAILURE_COUNT=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "
SELECT COUNT(*)
FROM batch_job_execution
WHERE status != 'COMPLETED'
  AND start_time >= NOW() - INTERVAL '7 days';
" | xargs)

if [ "$FAILURE_COUNT" -gt 0 ]; then
    echo -e "${RED}Found $FAILURE_COUNT failures:${NC}"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << 'EOF'
SELECT
    ji.job_name,
    je.job_execution_id,
    TO_CHAR(je.start_time, 'YYYY-MM-DD HH24:MI') as started_at,
    je.status,
    LEFT(je.exit_message, 80) as error_message
FROM batch_job_execution je
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
WHERE je.status != 'COMPLETED'
  AND je.start_time >= NOW() - INTERVAL '7 days'
ORDER BY je.start_time DESC
LIMIT 5;
EOF
else
    echo -e "${GREEN}✅ No failures in the last 7 days${NC}"
fi
echo ""

# 7. 문제가 있는 회사 확인
echo -e "${YELLOW}7️⃣  Companies with Potential Issues${NC}"
echo "--------------------------------------------"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << 'EOF'
SELECT
    c.name,
    COUNT(p.id) as posts,
    TO_CHAR(MAX(p.published_at), 'YYYY-MM-DD') as last_post,
    EXTRACT(DAY FROM (NOW() - MAX(p.published_at)))::INTEGER as days_ago,
    CASE
        WHEN COUNT(p.id) = 0 THEN '🔴 NO DATA'
        WHEN EXTRACT(DAY FROM (NOW() - MAX(p.published_at))) > 30 THEN '🔴 STALE'
        WHEN EXTRACT(DAY FROM (NOW() - MAX(p.published_at))) > 7 THEN '🟡 WARNING'
        ELSE '✅ OK'
    END as issue
FROM company c
LEFT JOIN post p ON p.company_id = c.id
GROUP BY c.id, c.name
HAVING COUNT(p.id) = 0 OR EXTRACT(DAY FROM (NOW() - MAX(p.published_at))) > 7
ORDER BY
    CASE
        WHEN COUNT(p.id) = 0 THEN 1
        ELSE 2
    END,
    EXTRACT(DAY FROM (NOW() - MAX(p.published_at))) DESC NULLS LAST;
EOF
echo ""

# 8. 종합 점수
echo -e "${YELLOW}8️⃣  Overall Health Score${NC}"
echo "--------------------------------------------"

# 성공률 계산
SUCCESS_RATE=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "
SELECT ROUND(
    COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END)::NUMERIC /
    NULLIF(COUNT(*), 0) * 100,
    2
)
FROM batch_job_execution
WHERE start_time >= NOW() - INTERVAL '7 days';
" | xargs)

# 데이터 신선도 계산
FRESH_COMPANIES=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "
SELECT COUNT(DISTINCT c.id)
FROM company c
JOIN post p ON p.company_id = c.id
WHERE p.published_at >= NOW() - INTERVAL '7 days';
" | xargs)

TOTAL_COMPANIES=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "
SELECT COUNT(*) FROM company;
" | xargs)

# 요약 완료율
SUMMARY_RATE=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "
SELECT ROUND(
    COUNT(CASE WHEN is_summary = true THEN 1 END)::NUMERIC /
    NULLIF(COUNT(*), 0) * 100,
    2
)
FROM post;
" | xargs)

echo "Batch Success Rate (7d): ${SUCCESS_RATE}%"
echo "Fresh Companies (7d): ${FRESH_COMPANIES}/${TOTAL_COMPANIES}"
echo "Summary Completion Rate: ${SUMMARY_RATE}%"
echo ""

# 점수 계산 (간단한 버전)
SCORE=0

# 성공률 점수 (최대 40점)
if (( $(echo "$SUCCESS_RATE >= 95" | bc -l) )); then
    SCORE=$((SCORE + 40))
elif (( $(echo "$SUCCESS_RATE >= 80" | bc -l) )); then
    SCORE=$((SCORE + 30))
elif (( $(echo "$SUCCESS_RATE >= 70" | bc -l) )); then
    SCORE=$((SCORE + 20))
else
    SCORE=$((SCORE + 10))
fi

# 신선도 점수 (최대 30점)
FRESHNESS_RATE=$((FRESH_COMPANIES * 100 / TOTAL_COMPANIES))
if (( FRESHNESS_RATE >= 90 )); then
    SCORE=$((SCORE + 30))
elif (( FRESHNESS_RATE >= 70 )); then
    SCORE=$((SCORE + 20))
elif (( FRESHNESS_RATE >= 50 )); then
    SCORE=$((SCORE + 10))
fi

# 요약 완료율 점수 (최대 30점)
if (( $(echo "$SUMMARY_RATE >= 90" | bc -l) )); then
    SCORE=$((SCORE + 30))
elif (( $(echo "$SUMMARY_RATE >= 70" | bc -l) )); then
    SCORE=$((SCORE + 20))
elif (( $(echo "$SUMMARY_RATE >= 50" | bc -l) )); then
    SCORE=$((SCORE + 10))
fi

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Overall Health Score: ${SCORE}/100${NC}"

if (( SCORE >= 80 )); then
    echo -e "${GREEN}Status: ✅ EXCELLENT${NC}"
elif (( SCORE >= 60 )); then
    echo -e "${YELLOW}Status: ⚡ GOOD (Minor improvements needed)${NC}"
elif (( SCORE >= 40 )); then
    echo -e "${YELLOW}Status: ⚠️  FAIR (Improvements recommended)${NC}"
else
    echo -e "${RED}Status: 🔴 POOR (Immediate attention required)${NC}"
fi
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${GREEN}✅ Baseline check complete!${NC}"
echo ""
echo "Next steps:"
echo "  1. Export detailed metrics: ./scripts/export_batch_metadata.sql"
echo "  2. Analyze company performance: ./scripts/analyze_company_performance.sql"
echo "  3. Check batch logs: tail -f /var/log/batch/batch.log"
echo ""
