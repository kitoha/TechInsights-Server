-- =========================================

-- TechInsights 성능 테스트 데이터 생성 스크립트

-- =========================================

--

-- 목적: 성능 테스트를 위한 Mock 데이터 생성

-- 특징: PERF_TEST_ 접두사로 테스트 데이터 구분

--

-- 사용법:

-- psql -h localhost -p 5434 -U perf_test_user -d techinsights_perf_test -f setup-test-data.sql



\echo '========================================='

\echo 'Performance Test Data Setup Started'

\echo '========================================='



-- 1. 기존 테스트 데이터 정리

\echo 'Step 1: Cleaning up old test data...'



DELETE FROM post_embedding WHERE company_name LIKE 'PERF_TEST_%';

DELETE FROM posts WHERE title LIKE 'PERF_TEST_%';

DELETE FROM company WHERE name LIKE 'PERF_TEST_%';



\echo 'Old test data cleaned up.'



-- 2. 테스트용 회사 생성

\echo 'Step 2: Creating test companies...'



INSERT INTO company (id, name, blog_url, logo_image_name, post_count, created_at, updated_at)

VALUES

  (gen_random_uuid(), 'PERF_TEST_Company1', 'https://perftest.com/feed1', 'perf_test_1.png', 0, NOW(), NOW()),

  (gen_random_uuid(), 'PERF_TEST_Company2', 'https://perftest.com/feed2', 'perf_test_2.png', 0, NOW(), NOW());



\echo 'Test companies created: 2'



-- 3. 테스트용 포스트 생성 (기본 100건)

\echo 'Step 3: Creating test posts...'



DO $$

DECLARE

company_id_1 UUID;

    company_id_2 UUID;

    record_count INT := 100;  -- 생성할 레코드 수 (필요시 변경)

BEGIN

    -- 회사 ID 조회

SELECT id INTO company_id_1 FROM company WHERE name = 'PERF_TEST_Company1' LIMIT 1;

SELECT id INTO company_id_2 FROM company WHERE name = 'PERF_TEST_Company2' LIMIT 1;



-- 포스트 생성

FOR i IN 1..record_count LOOP

        INSERT INTO posts (

            id,

            title,

            content,

            url,

            published_at,

            company_id,

            is_summary,

            is_embedding,

            view_count,

            created_at,

            updated_at

        )

        VALUES (

            gen_random_uuid(),

            'PERF_TEST_Post_' || i,

            'PERF_TEST_Content_' || i || ': ' || repeat('This is a long test content for performance testing. ', 50),

            'https://perftest.com/post/' || i,

            NOW() - (i || ' hours')::interval,

            CASE WHEN i % 2 = 0 THEN company_id_1 ELSE company_id_2 END,

            false,  -- 아직 요약 안됨

            false,  -- 아직 임베딩 안됨

            0,

            NOW(),

            NOW()

        );

END LOOP;



    RAISE NOTICE 'Test posts created: %', record_count;

END $$;



-- 4. 생성된 데이터 확인

\echo 'Step 4: Verification...'



SELECT

    'Companies' as type,

    COUNT(*) as count

FROM company

WHERE name LIKE 'PERF_TEST_%'



UNION ALL



SELECT

    'Posts (Not Summarized)' as type,

    COUNT(*) as count

FROM posts

WHERE title LIKE 'PERF_TEST_%'

  AND is_summary = false;



\echo '========================================='

\echo 'Performance Test Data Setup Completed!'

\echo '========================================='

\echo 'You can now run the batch job with:'

\echo './gradlew :batch:bootRun --args="--spring.profiles.active=perf-test --job.name=summaryAndEmbeddingJob"'



6. 성능 테스트 실행 스크립트
Write
/Users/kitoha/Desktop/Projects/kotlin/TechInsights/TechInsights-Server/scripts/performance-test/run-perf-test.sh
#!/bin/bash



# =========================================

# TechInsights 성능 테스트 실행 스크립트

# =========================================

#

# 사용법:

# ./run-perf-test.sh [레코드 수]

# 예: ./run-perf-test.sh 100



set -e  # 에러 발생 시 스크립트 중단



RECORD_COUNT=${1:-100}  # 기본값 100건

DB_HOST="localhost"

DB_PORT="5434"

DB_NAME="techinsights_perf_test"

DB_USER="perf_test_user"

PGPASSWORD="perf_test_password"



export PGPASSWORD



echo "========================================="

echo "TechInsights Performance Test"

echo "========================================="

echo "Record Count: $RECORD_COUNT"

echo "Start Time: $(date)"

echo ""



# 1. Docker Compose로 DB 시작

echo "Step 1: Starting performance test database..."

docker-compose -f docker-compose.performance-test.yml up -d



# 2. DB 준비 대기

echo "Step 2: Waiting for database to be ready..."

for i in {1..30}; do

if docker exec techinsights-perf-test-db pg_isready -U $DB_USER > /dev/null 2>&1; then

        echo "Database is ready!"

        break

    fi

    if [ $i -eq 30 ]; then

        echo "ERROR: Database failed to start within 30 seconds"

        exit 1

    fi

    echo "Waiting... ($i/30)"

    sleep 1

done



# 3. 스키마 초기화 대기 (추가 5초)

sleep 5



# 4. 테스트 데이터 생성

echo ""

echo "Step 3: Creating test data ($RECORD_COUNT records)..."



# SQL 파일의 record_count 변수 수정

sed "s/record_count INT := [0-9]*;/record_count INT := $RECORD_COUNT;/" \

    scripts/performance-test/setup-test-data.sql > /tmp/setup-test-data-temp.sql



psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f /tmp/setup-test-data-temp.sql



rm /tmp/setup-test-data-temp.sql



# 5. 배치 실행

echo ""

echo "Step 4: Running batch job..."

echo "========================================="



BATCH_START=$(date +%s)



./gradlew :batch:bootRun --args='--spring.profiles.active=perf-test --job.name=summaryAndEmbeddingJob' 2>&1 | tee /tmp/batch-perf-test.log



BATCH_END=$(date +%s)

BATCH_DURATION=$((BATCH_END - BATCH_START))



# 6. 결과 수집

echo ""

echo "========================================="

echo "Step 5: Collecting results..."

echo "========================================="



psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME <<EOF

SELECT

    'Total Posts' as metric,

    COUNT(*)::TEXT as value

FROM posts

WHERE title LIKE 'PERF_TEST_%'



UNION ALL



SELECT

    'Summarized Posts' as metric,

    COUNT(*)::TEXT as value

FROM posts

WHERE title LIKE 'PERF_TEST_%'

  AND is_summary = true



UNION ALL



SELECT

    'Embedded Posts' as metric,

    COUNT(*)::TEXT as value

FROM posts

WHERE title LIKE 'PERF_TEST_%'

  AND is_embedding = true



UNION ALL



SELECT

    'Embeddings Created' as metric,

    COUNT(*)::TEXT as value

FROM post_embedding

WHERE company_name LIKE 'PERF_TEST_%';

EOF



# 7. SQL 쿼리 통계 (UPDATE 쿼리 카운트)

echo ""

echo "========================================="

echo "SQL Query Statistics:"

echo "========================================="

grep -i "update posts" /tmp/batch-perf-test.log | wc -l | xargs echo "UPDATE queries executed:"



# 8. 요약

echo ""

echo "========================================="

echo "Performance Test Summary"

echo "========================================="

echo "Record Count: $RECORD_COUNT"

echo "Batch Duration: ${BATCH_DURATION}s"

echo "End Time: $(date)"

echo ""

echo "Cleanup command:"

echo "docker-compose -f docker-compose.performance-test.yml down -v"

echo "========================================="