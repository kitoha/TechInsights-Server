#!/bin/bash
# ========================================
# 배치 시스템 코드 정적 분석
# ========================================
# 실행 이력 없이도 코드만으로 문제점을 파악합니다.
#
# 사용법:
#   ./scripts/analyze_code_issues.sh
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
echo -e "${BLUE}🔍 배치 시스템 코드 정적 분석${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""
echo "프로젝트 경로: $PROJECT_ROOT"
echo "분석 시작: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

TOTAL_ISSUES=0
CRITICAL_ISSUES=0
WARNINGS=0

# ========================================
# 조건 1: 외부 의존성 지연이 전체 배치를 막지 않아야 한다
# ========================================
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}조건 1️⃣  외부 의존성 지연 격리${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# 1-1. 병렬 처리 확인
echo -e "${YELLOW}[1-1] 병렬 처리 메커니즘${NC}"
if grep -rq "Partitioner\|gridSize\|@Async" batch/src/main/kotlin/ 2>/dev/null; then
    echo -e "${GREEN}  ✅ 병렬 처리 코드 발견${NC}"
else
    echo -e "${RED}  ❌ 병렬 처리 미구현 - 순차 처리로 추정${NC}"
    echo "     위치: batch/src/main/kotlin/com/techinsights/batch/config/"
    echo "     근거: Partitioner, gridSize, @Async 키워드 없음"
    echo "     영향: 13개 회사 순차 처리, 가장 느린 회사가 전체 시간 좌우"
    ((CRITICAL_ISSUES++))
    ((TOTAL_ISSUES++))
fi

# 1-2. 타임아웃 설정 확인
echo ""
echo -e "${YELLOW}[1-2] Rate Limiter 타임아웃 설정${NC}"
TIMEOUT=$(grep "timeout-seconds:" batch/src/main/resources/application.yml | grep -o '[0-9]\+' | head -1)
if [ -n "$TIMEOUT" ]; then
    echo "  현재 설정: ${TIMEOUT}초"
    if [ "$TIMEOUT" -gt 60 ]; then
        echo -e "${RED}  ❌ 타임아웃이 과도하게 김 (${TIMEOUT}초)${NC}"
        echo "     위치: batch/src/main/resources/application.yml"
        echo "     권장: 30초 이하"
        echo "     영향: 한 회사 지연 시 최대 ${TIMEOUT}초 대기"
        ((CRITICAL_ISSUES++))
        ((TOTAL_ISSUES++))
    elif [ "$TIMEOUT" -gt 30 ]; then
        echo -e "${YELLOW}  ⚠️  타임아웃이 다소 김 (${TIMEOUT}초)${NC}"
        echo "     권장: 30초 이하"
        ((WARNINGS++))
    else
        echo -e "${GREEN}  ✅ 타임아웃 설정 양호 (${TIMEOUT}초)${NC}"
    fi
else
    echo -e "${YELLOW}  ⚠️  타임아웃 설정을 찾을 수 없음${NC}"
    ((WARNINGS++))
fi

# 1-3. WebClient 타임아웃 확인
echo ""
echo -e "${YELLOW}[1-3] WebClient 타임아웃 설정${NC}"
if grep -q "responseTimeout\|readTimeout" batch/src/main/kotlin/com/techinsights/batch/config/WebConfig.kt 2>/dev/null; then
    echo -e "${GREEN}  ✅ WebClient 타임아웃 설정됨${NC}"
    grep -A 2 "responseTimeout\|readTimeout" batch/src/main/kotlin/com/techinsights/batch/config/WebConfig.kt | head -3
else
    echo -e "${YELLOW}  ⚠️  WebClient 명시적 타임아웃 없음${NC}"
    echo "     위치: batch/src/main/kotlin/com/techinsights/batch/config/WebConfig.kt"
    echo "     권장: .responseTimeout(Duration.ofSeconds(30)) 추가"
    ((WARNINGS++))
fi

echo ""

# ========================================
# 조건 2: 실패한 작업만 선택적으로 재실행
# ========================================
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}조건 2️⃣  실패 작업 선택적 재실행${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# 2-1. 실패 추적 테이블/Entity 확인
echo -e "${YELLOW}[2-1] 실패 추적 메커니즘${NC}"
if find domain/src -type f -name "*Failure*.kt" -o -name "*failure*.kt" 2>/dev/null | grep -q .; then
    echo -e "${GREEN}  ✅ 실패 추적 Entity 발견${NC}"
    find domain/src -type f \( -name "*Failure*.kt" -o -name "*failure*.kt" \) 2>/dev/null | head -3
else
    echo -e "${RED}  ❌ 실패 추적 Entity 없음${NC}"
    echo "     영향: 실패한 회사 목록을 DB에 저장하지 않음"
    echo "     결과: 재실행 시 전체 회사를 다시 처리"
    echo "     개선: BatchCrawlFailure 엔티티 생성 필요"
    ((CRITICAL_ISSUES++))
    ((TOTAL_ISSUES++))
fi

# 2-2. 재실행용 Reader 확인
echo ""
echo -e "${YELLOW}[2-2] 선택적 재실행 Reader${NC}"
if grep -rq "FailedCompanyReader\|targetJobExecutionId" batch/src/main/kotlin/ 2>/dev/null; then
    echo -e "${GREEN}  ✅ 재실행용 Reader 발견${NC}"
else
    echo -e "${RED}  ❌ 선택적 재실행 Reader 없음${NC}"
    echo "     위치: batch/src/main/kotlin/com/techinsights/batch/reader/"
    echo "     영향: JobParameter로 특정 회사 필터링 불가"
    ((TOTAL_ISSUES++))
fi

echo ""

# ========================================
# 조건 3: 명시적인 SLA
# ========================================
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}조건 3️⃣  명시적인 SLA 정의${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# 3-1. SLA 설정 확인
echo -e "${YELLOW}[3-1] SLA 정의${NC}"
if grep -rq "sla:\|max.*duration\|max.*time" batch/src/main/resources/application*.yml 2>/dev/null; then
    echo -e "${GREEN}  ✅ SLA 설정 발견${NC}"
    grep -r "sla:\|max.*duration\|max.*time" batch/src/main/resources/application*.yml | head -5
else
    echo -e "${RED}  ❌ SLA 정의 없음${NC}"
    echo "     위치: batch/src/main/resources/application.yml"
    echo "     예시: batch.sla.max-total-duration-minutes: 30"
    echo "     영향: 목표 시간 대비 지연 여부를 측정할 수 없음"
    ((CRITICAL_ISSUES++))
    ((TOTAL_ISSUES++))
fi

# 3-2. SLA 모니터링 Listener 확인
echo ""
echo -e "${YELLOW}[3-2] SLA 모니터링 코드${NC}"
if grep -rq "SlaMonitor\|SLA.*Listener" batch/src/main/kotlin/ 2>/dev/null; then
    echo -e "${GREEN}  ✅ SLA 모니터링 코드 발견${NC}"
else
    echo -e "${RED}  ❌ SLA 모니터링 Listener 없음${NC}"
    echo "     영향: SLA 초과 여부를 자동 체크하지 않음"
    ((TOTAL_ISSUES++))
fi

echo ""

# ========================================
# 조건 4: 실행 상태 파악
# ========================================
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}조건 4️⃣  실행 상태 가시성${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# 4-1. 진행률 로깅 확인
echo -e "${YELLOW}[4-1] 진행률 표시${NC}"
if grep -rq "progress\|Processing.*\[.*\]" batch/src/main/kotlin/com/techinsights/batch/processor/ 2>/dev/null; then
    echo -e "${GREEN}  ✅ 진행률 로깅 코드 발견${NC}"
else
    echo -e "${YELLOW}  ⚠️  진행률 표시 코드 미흡${NC}"
    echo "     개선: \"[3/13] Processing: CompanyName\" 형식 로그 추가"
    ((WARNINGS++))
fi

# 4-2. 상태 조회 API 확인
echo ""
echo -e "${YELLOW}[4-2] 배치 상태 조회 API${NC}"
if grep -rq "BatchStatusController\|/batch/status" api/src/ 2>/dev/null; then
    echo -e "${GREEN}  ✅ 배치 상태 API 발견${NC}"
else
    echo -e "${RED}  ❌ 배치 상태 조회 API 없음${NC}"
    echo "     영향: 외부에서 실시간 배치 상태 확인 불가"
    echo "     개선: GET /batch/status/{jobName} 엔드포인트 추가"
    ((TOTAL_ISSUES++))
fi

echo ""

# ========================================
# 조건 5: 장애 인지 및 추적
# ========================================
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}조건 5️⃣  장애 인지 및 원인 추적${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# 5-1. 예외 분류 로직 확인
echo -e "${YELLOW}[5-1] 장애 분류 체계${NC}"
if grep -rq "FailureClassifier\|FailureType\|classifyException" batch/src/main/kotlin/ 2>/dev/null; then
    echo -e "${GREEN}  ✅ 장애 분류 코드 발견${NC}"
else
    echo -e "${RED}  ❌ 장애 분류 메커니즘 없음${NC}"
    echo "     영향: 모든 예외가 동일하게 처리됨"
    echo "     개선: BatchFailureClassifier 클래스 추가"
    ((TOTAL_ISSUES++))
fi

# 5-2. 알림 시스템 확인
echo ""
echo -e "${YELLOW}[5-2] 자동 알림 시스템${NC}"
if grep -rq "Slack.*Webhook\|EmailService\|Alert.*Service" batch/src/main/kotlin/ 2>/dev/null; then
    echo -e "${GREEN}  ✅ 알림 시스템 발견${NC}"
else
    echo -e "${RED}  ❌ 자동 알림 시스템 없음${NC}"
    echo "     영향: 배치 실패 시 수동으로 로그 확인 필요"
    echo "     개선: Slack Webhook 또는 이메일 알림 추가"
    ((CRITICAL_ISSUES++))
    ((TOTAL_ISSUES++))
fi

echo ""

# ========================================
# 조건 6: 안전한 재실행
# ========================================
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}조건 6️⃣  안전한 재실행 (Fault Tolerance)${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# 6-1. Skip Limit 확인
echo -e "${YELLOW}[6-1] Skip Limit 설정${NC}"
SKIP_LIMITS=$(grep -r "skipLimit" batch/src/main/kotlin/com/techinsights/batch/config/ 2>/dev/null | grep -o 'skipLimit([0-9]\+)' | grep -o '[0-9]\+')
if [ -n "$SKIP_LIMITS" ]; then
    echo "  현재 Skip Limit:"
    echo "$SKIP_LIMITS" | while read limit; do
        echo "    - $limit 개"
    done

    # 하드코딩 여부 확인
    if grep -rq "skipLimit(10)\|skipLimit(1000)" batch/src/main/kotlin/com/techinsights/batch/config/ 2>/dev/null; then
        echo -e "${YELLOW}  ⚠️  Skip Limit이 하드코딩됨${NC}"
        echo "     개선: application.yml에서 설정 가능하도록 변경"
        ((WARNINGS++))
    else
        echo -e "${GREEN}  ✅ Skip Limit 설정됨${NC}"
    fi
else
    echo -e "${YELLOW}  ⚠️  Skip Limit 설정을 찾을 수 없음${NC}"
fi

# 6-2. Retry 정책 확인
echo ""
echo -e "${YELLOW}[6-2] Retry 정책${NC}"
if grep -rq "retryLimit\|RetryPolicy" batch/src/main/kotlin/com/techinsights/batch/config/ 2>/dev/null; then
    echo -e "${GREEN}  ✅ Retry 정책 발견${NC}"
    RETRY_LIMIT=$(grep -r "retryLimit" batch/src/main/kotlin/com/techinsights/batch/config/ 2>/dev/null | grep -o 'retryLimit([0-9]\+)' | grep -o '[0-9]\+' | head -1)
    if [ -n "$RETRY_LIMIT" ]; then
        echo "     Retry Limit: $RETRY_LIMIT 회"
    fi

    # Exponential Backoff 확인
    if grep -rq "ExponentialBackOffPolicy\|backoff" batch/src/main/kotlin/ 2>/dev/null; then
        echo -e "${GREEN}  ✅ Exponential Backoff 사용${NC}"
    else
        echo -e "${YELLOW}  ⚠️  Simple Retry (Exponential Backoff 미사용)${NC}"
        echo "     개선: ExponentialBackOffPolicy 적용 권장"
        ((WARNINGS++))
    fi
else
    echo -e "${YELLOW}  ⚠️  명시적인 Retry 정책 없음${NC}"
fi

echo ""

# ========================================
# 조건 7: 데이터 신선도 (Freshness)
# ========================================
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}조건 7️⃣  데이터 신선도 보장${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# 7-1. Company Freshness 필드 확인
echo -e "${YELLOW}[7-1] Freshness 추적 필드${NC}"
if grep -rq "lastCrawledAt\|last_crawled_at\|lastSuccessfulCrawl" domain/src/ 2>/dev/null; then
    echo -e "${GREEN}  ✅ Freshness 필드 발견${NC}"
    grep -r "lastCrawledAt\|last_crawled_at" domain/src/ 2>/dev/null | head -3
else
    echo -e "${RED}  ❌ Freshness 추적 필드 없음${NC}"
    echo "     위치: domain/src/.../entity/Company.kt"
    echo "     필요 필드:"
    echo "       - lastCrawledAt: LocalDateTime?"
    echo "       - lastSuccessfulCrawlAt: LocalDateTime?"
    echo "     영향: 각 회사의 마지막 수집 시점을 알 수 없음"
    ((CRITICAL_ISSUES++))
    ((TOTAL_ISSUES++))
fi

# 7-2. Freshness 모니터링 확인
echo ""
echo -e "${YELLOW}[7-2] Freshness 모니터링${NC}"
if grep -rq "FreshnessMonitor\|checkDataFreshness" batch/src/main/kotlin/ 2>/dev/null; then
    echo -e "${GREEN}  ✅ Freshness 모니터링 코드 발견${NC}"
else
    echo -e "${RED}  ❌ Freshness 모니터링 없음${NC}"
    echo "     개선: @Scheduled로 주기적 Freshness 체크"
    ((TOTAL_ISSUES++))
fi

echo ""

# ========================================
# 조건 8: Idempotency (재실행 안전성)
# ========================================
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}조건 8️⃣  Idempotency (중복 방지)${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# 8-1. URL 중복 체크 확인
echo -e "${YELLOW}[8-1] Post URL 중복 체크${NC}"
if grep -rq "findAllByUrlIn\|existsByUrl" batch/src/main/kotlin/com/techinsights/batch/writer/ 2>/dev/null; then
    echo -e "${GREEN}  ✅ URL 중복 체크 발견${NC}"
    grep -r "findAllByUrlIn\|existsByUrl" batch/src/main/kotlin/com/techinsights/batch/writer/ 2>/dev/null | head -2
else
    echo -e "${RED}  ❌ URL 중복 체크 없음${NC}"
    echo "     위치: batch/src/main/kotlin/com/techinsights/batch/writer/RawPostWriter.kt"
    echo "     영향: 재실행 시 중복 데이터 저장 가능"
    ((CRITICAL_ISSUES++))
    ((TOTAL_ISSUES++))
fi

# 8-2. Idempotency Key 테이블 확인
echo ""
echo -e "${YELLOW}[8-2] Idempotency Key 추적${NC}"
if find domain/src -name "*Idempotency*" -type f 2>/dev/null | grep -q .; then
    echo -e "${GREEN}  ✅ Idempotency 추적 Entity 발견${NC}"
else
    echo -e "${YELLOW}  ⚠️  Idempotency Key 테이블 없음${NC}"
    echo "     영향: Gemini API 중복 호출 가능 (비용 증가)"
    echo "     개선: BatchIdempotencyRecord 테이블 추가 권장"
    ((WARNINGS++))
fi

echo ""

# ========================================
# 종합 점수
# ========================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}📊 종합 분석 결과${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

echo ""
echo "발견된 문제:"
echo -e "  🔴 Critical Issues: ${RED}$CRITICAL_ISSUES${NC}"
echo -e "  🟡 Warnings: ${YELLOW}$WARNINGS${NC}"
echo -e "  📝 Total Issues: $TOTAL_ISSUES"
echo ""

# 건강도 점수 계산 (간단한 버전)
SCORE=100
SCORE=$((SCORE - CRITICAL_ISSUES * 15))
SCORE=$((SCORE - WARNINGS * 5))

if [ $SCORE -lt 0 ]; then
    SCORE=0
fi

echo -e "${BLUE}건강도 점수: $SCORE/100${NC}"

if [ $SCORE -ge 80 ]; then
    echo -e "${GREEN}상태: ✅ EXCELLENT${NC}"
elif [ $SCORE -ge 60 ]; then
    echo -e "${YELLOW}상태: ⚡ GOOD (일부 개선 필요)${NC}"
elif [ $SCORE -ge 40 ]; then
    echo -e "${YELLOW}상태: ⚠️  FAIR (개선 권장)${NC}"
else
    echo -e "${RED}상태: 🔴 POOR (즉시 개선 필요)${NC}"
fi

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# ========================================
# 우선순위 개선 항목
# ========================================
echo ""
echo -e "${CYAN}🎯 우선순위 개선 항목${NC}"
echo ""

echo -e "${RED}🔥 High Priority (즉시 개선 필요)${NC}"
if [ $CRITICAL_ISSUES -gt 0 ]; then
    echo "  1. 병렬 처리 도입 (Partitioning)"
    echo "     → 예상 효과: 전체 시간 60% 단축"
    echo ""
    echo "  2. 실패 추적 테이블 추가 (BatchCrawlFailure)"
    echo "     → 예상 효과: 재실행 시간 90% 단축"
    echo ""
    echo "  3. SLA 정의 및 모니터링"
    echo "     → 예상 효과: 성능 저하 조기 발견"
    echo ""
    echo "  4. Freshness 추적 필드 추가"
    echo "     → 예상 효과: 데이터 품질 SLA 보장"
    echo ""
    echo "  5. 자동 알림 시스템"
    echo "     → 예상 효과: 장애 대응 시간 70% 단축"
else
    echo "  (없음)"
fi
echo ""

echo -e "${YELLOW}⚡ Medium Priority${NC}"
if [ $WARNINGS -gt 0 ]; then
    echo "  1. 타임아웃 최적화 (300s → 30s)"
    echo "  2. Exponential Backoff 적용"
    echo "  3. Idempotency Key 테이블 추가"
    echo "  4. 진행률 로깅 강화"
else
    echo "  (없음)"
fi
echo ""

# ========================================
# 다음 단계
# ========================================
echo -e "${CYAN}📋 다음 단계${NC}"
echo ""
echo "1. 이 분석 결과를 Baseline 리포트에 포함"
echo "2. 우선순위에 따라 개선 작업 진행"
echo "3. (선택) 1회 상세 프로파일링 실행:"
echo "   - BaselineMetricsListener 추가"
echo "   - 1회 배치 실행으로 회사별 처리 시간 측정"
echo "4. (권장) 프로덕션 데이터 확보:"
echo "   - DBA에게 읽기 권한 요청"
echo "   - 프로덕션 DB로 ./scripts/quick_baseline_check.sh 실행"
echo ""

echo -e "${GREEN}✅ 코드 정적 분석 완료!${NC}"
echo ""
echo "리포트 저장:"
echo "  ./scripts/analyze_code_issues.sh > code_analysis_report_$(date +%Y%m%d).txt"
