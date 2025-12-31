-- ========================================
-- 회사별 크롤링 성능 분석 SQL
-- ========================================
-- 이 스크립트는 Company 테이블과 Post 테이블을 분석하여
-- 회사별 데이터 수집 현황을 파악합니다.
-- ========================================

\echo '========================================='
\echo '🏢 Company Crawling Performance Analysis'
\echo '========================================='
\echo ''

-- 1. 회사별 게시글 수집 통계
\echo '1. Posts Collection Statistics by Company'
\echo '-----------------------------------------'

SELECT
    c.name AS "Company",
    c.blog_url AS "Blog URL",
    c.rss_supported AS "RSS Support",
    c.post_count AS "Total Posts",
    c.total_view_count AS "Total Views",
    COUNT(p.id) AS "Actual Posts in DB",
    TO_CHAR(MAX(p.published_at), 'YYYY-MM-DD') AS "Latest Post Date",
    TO_CHAR(MIN(p.published_at), 'YYYY-MM-DD') AS "Oldest Post Date",
    EXTRACT(DAY FROM (NOW() - MAX(p.published_at))) AS "Days Since Last Post",
    CASE
        WHEN EXTRACT(DAY FROM (NOW() - MAX(p.published_at))) > 7 THEN '⚠️ Stale'
        WHEN EXTRACT(DAY FROM (NOW() - MAX(p.published_at))) > 3 THEN '⚡ Warning'
        ELSE '✅ Fresh'
    END AS "Data Freshness"
FROM company c
LEFT JOIN post p ON p.company_id = c.id
GROUP BY c.id, c.name, c.blog_url, c.rss_supported, c.post_count, c.total_view_count
ORDER BY COUNT(p.id) DESC;

\echo ''
\echo ''

-- 2. 요약 및 임베딩 진행 현황
\echo '2. Summary & Embedding Progress'
\echo '-----------------------------------------'

SELECT
    c.name AS "Company",
    COUNT(p.id) AS "Total Posts",
    COUNT(CASE WHEN p.is_summary = true THEN 1 END) AS "✅ Summarized",
    COUNT(CASE WHEN p.is_summary = false THEN 1 END) AS "⏳ Pending Summary",
    COUNT(CASE WHEN p.is_embedding = true THEN 1 END) AS "✅ Embedded",
    COUNT(CASE WHEN p.is_embedding = false THEN 1 END) AS "⏳ Pending Embedding",
    ROUND(
        COUNT(CASE WHEN p.is_summary = true THEN 1 END)::NUMERIC /
        NULLIF(COUNT(p.id), 0) * 100,
        2
    ) AS "Summary %",
    ROUND(
        COUNT(CASE WHEN p.is_embedding = true THEN 1 END)::NUMERIC /
        NULLIF(COUNT(p.id), 0) * 100,
        2
    ) AS "Embedding %"
FROM company c
LEFT JOIN post p ON p.company_id = c.id
GROUP BY c.id, c.name
ORDER BY COUNT(p.id) DESC;

\echo ''
\echo ''

-- 3. 데이터 신선도 상세 (Freshness Detail)
\echo '3. Data Freshness Detail'
\echo '-----------------------------------------'

WITH freshness_data AS (
    SELECT
        c.id,
        c.name,
        MAX(p.published_at) AS last_post_date,
        EXTRACT(DAY FROM (NOW() - MAX(p.published_at))) AS days_old,
        COUNT(p.id) FILTER (WHERE p.published_at >= NOW() - INTERVAL '7 days') AS posts_last_7d,
        COUNT(p.id) FILTER (WHERE p.published_at >= NOW() - INTERVAL '30 days') AS posts_last_30d,
        COUNT(p.id) AS total_posts
    FROM company c
    LEFT JOIN post p ON p.company_id = c.id
    GROUP BY c.id, c.name
)
SELECT
    name AS "Company",
    TO_CHAR(last_post_date, 'YYYY-MM-DD HH24:MI') AS "Last Post",
    CONCAT(days_old, ' days ago') AS "Age",
    posts_last_7d AS "Posts (7d)",
    posts_last_30d AS "Posts (30d)",
    total_posts AS "Total Posts",
    CASE
        WHEN days_old IS NULL THEN '❌ No Data'
        WHEN days_old > 30 THEN '🔴 Very Stale'
        WHEN days_old > 7 THEN '🟡 Stale'
        WHEN days_old > 3 THEN '🟢 Acceptable'
        ELSE '✅ Fresh'
    END AS "Freshness Status",
    CASE
        WHEN days_old > 30 THEN 'CRITICAL: No updates for 1 month'
        WHEN days_old > 7 THEN 'WARNING: No updates for 1 week'
        WHEN posts_last_7d = 0 THEN 'INFO: No posts this week'
        ELSE 'OK'
    END AS "Alert"
FROM freshness_data
ORDER BY days_old DESC NULLS LAST;

\echo ''
\echo ''

-- 4. 조회수 통계 (Top Performing Companies)
\echo '4. Top Companies by View Count'
\echo '-----------------------------------------'

SELECT
    c.name AS "Company",
    c.total_view_count AS "Total Views",
    c.post_count AS "Posts",
    ROUND(c.total_view_count::NUMERIC / NULLIF(c.post_count, 0), 2) AS "Avg Views per Post",
    COUNT(pv.id) AS "Actual View Records",
    COUNT(DISTINCT pv.user_or_ip) AS "Unique Viewers"
FROM company c
LEFT JOIN post p ON p.company_id = c.id
LEFT JOIN post_view pv ON pv.post_id = p.id
GROUP BY c.id, c.name, c.total_view_count, c.post_count
ORDER BY c.total_view_count DESC
LIMIT 10;

\echo ''
\echo ''

-- 5. 게시 빈도 분석 (Posting Frequency)
\echo '5. Posting Frequency Analysis'
\echo '-----------------------------------------'

WITH posting_stats AS (
    SELECT
        c.name,
        COUNT(p.id) AS total_posts,
        MIN(p.published_at) AS first_post,
        MAX(p.published_at) AS last_post,
        EXTRACT(DAY FROM (MAX(p.published_at) - MIN(p.published_at))) AS days_span
    FROM company c
    LEFT JOIN post p ON p.company_id = c.id
    WHERE p.published_at IS NOT NULL
    GROUP BY c.id, c.name
)
SELECT
    name AS "Company",
    total_posts AS "Total Posts",
    TO_CHAR(first_post, 'YYYY-MM-DD') AS "First Post",
    TO_CHAR(last_post, 'YYYY-MM-DD') AS "Last Post",
    days_span AS "Days Span",
    CASE
        WHEN days_span > 0 THEN ROUND(total_posts::NUMERIC / days_span, 2)
        ELSE 0
    END AS "Posts per Day",
    CASE
        WHEN days_span > 0 THEN ROUND(total_posts::NUMERIC / (days_span / 7.0), 2)
        ELSE 0
    END AS "Posts per Week"
FROM posting_stats
WHERE total_posts > 0
ORDER BY "Posts per Week" DESC;

\echo ''
\echo ''

-- 6. 크롤링 실패 추정 (회사별)
\echo '6. Potential Crawling Issues'
\echo '-----------------------------------------'

WITH company_health AS (
    SELECT
        c.name,
        c.rss_supported,
        COUNT(p.id) AS total_posts,
        MAX(p.published_at) AS last_successful_crawl,
        EXTRACT(DAY FROM (NOW() - MAX(p.published_at))) AS days_since_last,
        COUNT(p.id) FILTER (WHERE p.is_summary = false) AS pending_summary,
        COUNT(p.id) FILTER (WHERE p.is_embedding = false) AS pending_embedding
    FROM company c
    LEFT JOIN post p ON p.company_id = c.id
    GROUP BY c.id, c.name, c.rss_supported
)
SELECT
    name AS "Company",
    rss_supported AS "RSS?",
    total_posts AS "Posts",
    TO_CHAR(last_successful_crawl, 'YYYY-MM-DD') AS "Last Crawl",
    days_since_last AS "Days Ago",
    pending_summary AS "Pending Summary",
    pending_embedding AS "Pending Embed",
    CASE
        WHEN total_posts = 0 THEN '🔴 NEVER CRAWLED'
        WHEN days_since_last > 30 THEN '🔴 NOT CRAWLING'
        WHEN days_since_last > 7 THEN '🟡 SLOW UPDATES'
        WHEN pending_summary > 100 THEN '🟡 SUMMARY BACKLOG'
        WHEN pending_embedding > 100 THEN '🟡 EMBEDDING BACKLOG'
        ELSE '✅ HEALTHY'
    END AS "Health Status"
FROM company_health
ORDER BY
    CASE
        WHEN total_posts = 0 THEN 1
        WHEN days_since_last > 30 THEN 2
        WHEN days_since_last > 7 THEN 3
        ELSE 4
    END,
    days_since_last DESC NULLS LAST;

\echo ''
\echo ''

-- 7. 카테고리 분포 (회사별)
\echo '7. Category Distribution by Company'
\echo '-----------------------------------------'

SELECT
    c.name AS "Company",
    COUNT(p.id) AS "Total Posts",
    COUNT(pc.category) FILTER (WHERE pc.category = 'BACKEND') AS "Backend",
    COUNT(pc.category) FILTER (WHERE pc.category = 'FRONTEND') AS "Frontend",
    COUNT(pc.category) FILTER (WHERE pc.category = 'AI') AS "AI",
    COUNT(pc.category) FILTER (WHERE pc.category = 'DATA') AS "Data",
    COUNT(pc.category) FILTER (WHERE pc.category = 'DEVOPS') AS "DevOps",
    COUNT(pc.category) FILTER (WHERE pc.category = 'ETC') AS "Etc"
FROM company c
LEFT JOIN post p ON p.company_id = c.id
LEFT JOIN post_categories pc ON pc.post_id = p.id
GROUP BY c.id, c.name
HAVING COUNT(p.id) > 0
ORDER BY COUNT(p.id) DESC;

\echo ''
\echo ''

-- 8. 중복 URL 체크 (Idempotency 검증)
\echo '8. Duplicate URL Check (Idempotency Verification)'
\echo '-----------------------------------------'

SELECT
    c.name AS "Company",
    p.url,
    COUNT(*) AS "Duplicate Count",
    string_agg(p.id::TEXT, ', ') AS "Post IDs"
FROM post p
JOIN company c ON p.company_id = c.id
GROUP BY c.name, p.url
HAVING COUNT(*) > 1
ORDER BY COUNT(*) DESC, c.name;

\echo ''
\echo ''

-- 9. 빈 컨텐츠 체크 (Quality Check)
\echo '9. Content Quality Check'
\echo '-----------------------------------------'

SELECT
    c.name AS "Company",
    COUNT(*) FILTER (WHERE p.content IS NULL OR p.content = '') AS "Empty Content",
    COUNT(*) FILTER (WHERE p.preview IS NULL OR p.preview = '') AS "Empty Preview",
    COUNT(*) FILTER (WHERE p.thumbnail IS NULL OR p.thumbnail = '') AS "No Thumbnail",
    COUNT(*) FILTER (
        WHERE (p.content IS NULL OR p.content = '')
          AND (p.preview IS NULL OR p.preview = '')
    ) AS "Both Empty",
    COUNT(*) AS "Total Posts",
    ROUND(
        COUNT(*) FILTER (WHERE p.content IS NULL OR p.content = '')::NUMERIC /
        NULLIF(COUNT(*), 0) * 100,
        2
    ) AS "Empty %"
FROM company c
LEFT JOIN post p ON p.company_id = c.id
GROUP BY c.id, c.name
HAVING COUNT(*) > 0
ORDER BY "Empty %" DESC;

\echo ''
\echo ''

-- 10. 요약 필요 (현재 Baseline 대비 개선 후 비교용)
\echo '10. Summary: Current State vs Target'
\echo '-----------------------------------------'

WITH metrics AS (
    SELECT
        COUNT(DISTINCT c.id) AS total_companies,
        COUNT(DISTINCT c.id) FILTER (WHERE EXISTS (
            SELECT 1 FROM post p WHERE p.company_id = c.id
        )) AS companies_with_data,
        COUNT(p.id) AS total_posts,
        COUNT(p.id) FILTER (WHERE p.is_summary = true) AS summarized_posts,
        COUNT(p.id) FILTER (WHERE p.is_embedding = true) AS embedded_posts,
        COUNT(DISTINCT c.id) FILTER (WHERE EXISTS (
            SELECT 1 FROM post p
            WHERE p.company_id = c.id
              AND p.published_at >= NOW() - INTERVAL '7 days'
        )) AS active_last_7d
    FROM company c
    LEFT JOIN post p ON p.company_id = c.id
)
SELECT
    total_companies AS "Total Companies",
    companies_with_data AS "Companies w/ Data",
    (total_companies - companies_with_data) AS "⚠️ Companies w/o Data",
    total_posts AS "Total Posts",
    summarized_posts AS "✅ Summarized",
    (total_posts - summarized_posts) AS "⏳ Pending Summary",
    embedded_posts AS "✅ Embedded",
    (total_posts - embedded_posts) AS "⏳ Pending Embedding",
    active_last_7d AS "Active (7d)",
    ROUND(summarized_posts::NUMERIC / NULLIF(total_posts, 0) * 100, 2) AS "Summary Coverage %",
    ROUND(embedded_posts::NUMERIC / NULLIF(total_posts, 0) * 100, 2) AS "Embedding Coverage %"
FROM metrics;

\echo ''
\echo ''
\echo '========================================='
\echo '✅ Company Analysis Complete!'
\echo '========================================='
