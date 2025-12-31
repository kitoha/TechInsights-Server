-- ========================================
-- Spring Batch 메타데이터 추출 SQL
-- ========================================
-- 사용법: psql -h localhost -U user -d techinsights -f export_batch_metadata.sql -o baseline_data.csv
--
-- 이 스크립트는 개선 전 Baseline 데이터를 수집합니다.
-- ========================================

\echo '========================================='
\echo '📊 Batch Baseline Data Export'
\echo '========================================='
\echo ''

-- 1. 최근 30일 Job 실행 통계
\echo '1. Job Execution Summary (Last 30 days)'
\echo '-----------------------------------------'

SELECT
    ji.job_name AS "Job Name",
    COUNT(*) AS "Total Runs",
    COUNT(CASE WHEN je.status = 'COMPLETED' THEN 1 END) AS "✅ Success",
    COUNT(CASE WHEN je.status = 'FAILED' THEN 1 END) AS "❌ Failed",
    COUNT(CASE WHEN je.status = 'STOPPED' THEN 1 END) AS "⏸️  Stopped",
    ROUND(
        COUNT(CASE WHEN je.status = 'COMPLETED' THEN 1 END)::NUMERIC /
        NULLIF(COUNT(*), 0) * 100,
        2
    ) AS "Success Rate %",
    ROUND(AVG(EXTRACT(EPOCH FROM (je.end_time - je.start_time))), 2) AS "Avg Duration (s)",
    ROUND(MAX(EXTRACT(EPOCH FROM (je.end_time - je.start_time))), 2) AS "Max Duration (s)",
    ROUND(MIN(EXTRACT(EPOCH FROM (je.end_time - je.start_time))), 2) AS "Min Duration (s)",
    TO_CHAR(MIN(je.start_time), 'YYYY-MM-DD HH24:MI') AS "First Run",
    TO_CHAR(MAX(je.start_time), 'YYYY-MM-DD HH24:MI') AS "Last Run"
FROM batch_job_execution je
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
WHERE je.create_time >= NOW() - INTERVAL '30 days'
GROUP BY ji.job_name
ORDER BY ji.job_name;

\echo ''
\echo ''

-- 2. Step별 성능 분석
\echo '2. Step Performance Analysis'
\echo '-----------------------------------------'

SELECT
    ji.job_name AS "Job",
    se.step_name AS "Step",
    COUNT(*) AS "Runs",
    ROUND(AVG(se.read_count), 2) AS "Avg Read",
    ROUND(AVG(se.write_count), 2) AS "Avg Write",
    SUM(se.skip_count) AS "Total Skipped",
    SUM(se.rollback_count) AS "Total Rollbacks",
    ROUND(
        AVG(EXTRACT(EPOCH FROM (se.end_time - se.start_time))),
        2
    ) AS "Avg Step Duration (s)",
    ROUND(
        AVG(se.write_count::NUMERIC / NULLIF(EXTRACT(EPOCH FROM (se.end_time - se.start_time)), 0)),
        2
    ) AS "Throughput (items/s)"
FROM batch_step_execution se
JOIN batch_job_execution je ON se.job_execution_id = je.job_execution_id
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
WHERE se.start_time >= NOW() - INTERVAL '30 days'
GROUP BY ji.job_name, se.step_name
ORDER BY ji.job_name, se.step_name;

\echo ''
\echo ''

-- 3. 최근 실패한 Job 상세 (최근 20개)
\echo '3. Recent Failed Jobs (Last 20)'
\echo '-----------------------------------------'

SELECT
    ji.job_name AS "Job",
    je.job_execution_id AS "Execution ID",
    TO_CHAR(je.start_time, 'YYYY-MM-DD HH24:MI:SS') AS "Started At",
    ROUND(EXTRACT(EPOCH FROM (je.end_time - je.start_time)), 2) AS "Duration (s)",
    je.status AS "Status",
    je.exit_code AS "Exit Code",
    LEFT(je.exit_message, 100) AS "Exit Message (truncated)",
    se.step_name AS "Failed Step",
    se.read_count AS "Read",
    se.write_count AS "Write",
    se.skip_count AS "Skip"
FROM batch_job_execution je
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
LEFT JOIN batch_step_execution se ON je.job_execution_id = se.job_execution_id
WHERE je.status != 'COMPLETED'
  AND je.start_time >= NOW() - INTERVAL '7 days'
ORDER BY je.start_time DESC
LIMIT 20;

\echo ''
\echo ''

-- 4. 일별 처리량 추이 (최근 30일)
\echo '4. Daily Processing Trend (Last 30 days)'
\echo '-----------------------------------------'

SELECT
    DATE(je.start_time) AS "Date",
    ji.job_name AS "Job",
    COUNT(*) AS "Runs",
    SUM(se.write_count) AS "Total Items Processed",
    ROUND(AVG(EXTRACT(EPOCH FROM (je.end_time - je.start_time))), 2) AS "Avg Duration (s)",
    COUNT(CASE WHEN je.status = 'COMPLETED' THEN 1 END) AS "Success",
    COUNT(CASE WHEN je.status = 'FAILED' THEN 1 END) AS "Failed"
FROM batch_job_execution je
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
LEFT JOIN batch_step_execution se ON je.job_execution_id = se.job_execution_id
WHERE je.start_time >= NOW() - INTERVAL '30 days'
GROUP BY DATE(je.start_time), ji.job_name
ORDER BY DATE(je.start_time) DESC, ji.job_name;

\echo ''
\echo ''

-- 5. Skip 발생 패턴 분석
\echo '5. Skip Pattern Analysis'
\echo '-----------------------------------------'

SELECT
    ji.job_name AS "Job",
    se.step_name AS "Step",
    COUNT(*) AS "Total Executions",
    SUM(se.skip_count) AS "Total Skips",
    ROUND(
        SUM(se.skip_count)::NUMERIC / NULLIF(SUM(se.read_count), 0) * 100,
        2
    ) AS "Skip Rate %",
    MAX(se.skip_count) AS "Max Skips in Single Run",
    TO_CHAR(MAX(se.start_time) FILTER (WHERE se.skip_count > 0), 'YYYY-MM-DD HH24:MI') AS "Last Skip Event"
FROM batch_step_execution se
JOIN batch_job_execution je ON se.job_execution_id = je.job_execution_id
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
WHERE se.start_time >= NOW() - INTERVAL '30 days'
GROUP BY ji.job_name, se.step_name
HAVING SUM(se.skip_count) > 0
ORDER BY SUM(se.skip_count) DESC;

\echo ''
\echo ''

-- 6. 성능 추이 분석 (주별)
\echo '6. Weekly Performance Trend'
\echo '-----------------------------------------'

SELECT
    DATE_TRUNC('week', je.start_time)::DATE AS "Week Starting",
    ji.job_name AS "Job",
    COUNT(*) AS "Runs",
    ROUND(AVG(EXTRACT(EPOCH FROM (je.end_time - je.start_time))), 2) AS "Avg Duration (s)",
    ROUND(STDDEV(EXTRACT(EPOCH FROM (je.end_time - je.start_time))), 2) AS "Std Dev (s)",
    ROUND(
        AVG(se.write_count::NUMERIC / NULLIF(EXTRACT(EPOCH FROM (se.end_time - se.start_time)), 0)),
        2
    ) AS "Avg Throughput (items/s)"
FROM batch_job_execution je
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
LEFT JOIN batch_step_execution se ON je.job_execution_id = se.job_execution_id
WHERE je.start_time >= NOW() - INTERVAL '30 days'
  AND je.status = 'COMPLETED'
GROUP BY DATE_TRUNC('week', je.start_time), ji.job_name
ORDER BY DATE_TRUNC('week', je.start_time) DESC, ji.job_name;

\echo ''
\echo ''

-- 7. 최악의 실행 (Top 10 slowest)
\echo '7. Top 10 Slowest Executions'
\echo '-----------------------------------------'

SELECT
    ji.job_name AS "Job",
    je.job_execution_id AS "Execution ID",
    TO_CHAR(je.start_time, 'YYYY-MM-DD HH24:MI:SS') AS "Started At",
    EXTRACT(EPOCH FROM (je.end_time - je.start_time)) AS "Duration (s)",
    ROUND(EXTRACT(EPOCH FROM (je.end_time - je.start_time)) / 60, 2) AS "Duration (min)",
    se.read_count AS "Read",
    se.write_count AS "Write",
    se.skip_count AS "Skip",
    je.status AS "Status"
FROM batch_job_execution je
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
LEFT JOIN batch_step_execution se ON je.job_execution_id = se.job_execution_id
WHERE je.start_time >= NOW() - INTERVAL '30 days'
  AND je.end_time IS NOT NULL
ORDER BY (je.end_time - je.start_time) DESC
LIMIT 10;

\echo ''
\echo ''

-- 8. 최고의 실행 (Top 10 fastest)
\echo '8. Top 10 Fastest Executions'
\echo '-----------------------------------------'

SELECT
    ji.job_name AS "Job",
    je.job_execution_id AS "Execution ID",
    TO_CHAR(je.start_time, 'YYYY-MM-DD HH24:MI:SS') AS "Started At",
    EXTRACT(EPOCH FROM (je.end_time - je.start_time)) AS "Duration (s)",
    se.read_count AS "Read",
    se.write_count AS "Write",
    je.status AS "Status"
FROM batch_job_execution je
JOIN batch_job_instance ji ON je.job_instance_id = ji.job_instance_id
LEFT JOIN batch_step_execution se ON je.job_execution_id = se.job_execution_id
WHERE je.start_time >= NOW() - INTERVAL '30 days'
  AND je.end_time IS NOT NULL
  AND je.status = 'COMPLETED'
  AND EXTRACT(EPOCH FROM (je.end_time - je.start_time)) > 0
ORDER BY (je.end_time - je.start_time) ASC
LIMIT 10;

\echo ''
\echo ''
\echo '========================================='
\echo '✅ Export Complete!'
\echo '========================================='
