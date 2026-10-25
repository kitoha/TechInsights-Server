-- ============================================================
-- posts.url 유일성을 DB가 보장하도록 중복 정리 후 UNIQUE 인덱스 추가
-- 남기는 행: 삭제되지 않은 행 → 요약된 행 → 임베딩된 행 우선, 그다음 created_at → published_at → id 가 가장 작은 행
-- (요약/임베딩 결과물을 가진 행을 남겨야 재요약 비용과 검색 누락이 생기지 않는다)
-- ============================================================

CREATE TEMP TABLE post_url_dedup ON COMMIT DROP AS
SELECT id AS dup_id, keep_id
FROM (
    SELECT id,
           FIRST_VALUE(id) OVER w AS keep_id,
           ROW_NUMBER()    OVER w AS rn
    FROM posts
    WINDOW w AS (
        PARTITION BY url
        ORDER BY (deleted_at IS NOT NULL), is_summary DESC, is_embedding DESC, created_at, published_at, id
    )
) ranked
WHERE rn > 1;

-- 사용자 데이터(좋아요): 남기는 글과 충돌하지 않는 행만 옮기고 like_count 에 반영
WITH ranked AS (
    SELECT pl.id, d.keep_id,
           ROW_NUMBER() OVER (
               PARTITION BY d.keep_id, pl.user_id,
                            CASE WHEN pl.user_id IS NULL THEN pl.ip_address END
               ORDER BY pl.created_at, pl.id
           ) AS rn
    FROM post_likes pl
    JOIN post_url_dedup d ON pl.post_id = d.dup_id
),
moved AS (
    UPDATE post_likes pl
    SET post_id = r.keep_id
    FROM ranked r
    WHERE pl.id = r.id
      AND r.rn = 1
      AND NOT EXISTS (
          SELECT 1
          FROM post_likes k
          WHERE k.post_id = r.keep_id
            AND k.user_id IS NOT DISTINCT FROM pl.user_id
            AND (pl.user_id IS NOT NULL OR k.ip_address = pl.ip_address)
      )
    RETURNING r.keep_id
)
UPDATE posts p
SET like_count = p.like_count + m.cnt
FROM (SELECT keep_id, COUNT(*) AS cnt FROM moved GROUP BY keep_id) m
WHERE p.id = m.keep_id;

DELETE FROM post_likes WHERE post_id IN (SELECT dup_id FROM post_url_dedup);

-- 사용자 데이터(북마크): UNIQUE (post_id, user_id) 충돌 시 삭제
WITH ranked AS (
    SELECT b.id, d.keep_id,
           ROW_NUMBER() OVER (PARTITION BY d.keep_id, b.user_id ORDER BY b.created_at, b.id) AS rn
    FROM post_bookmarks b
    JOIN post_url_dedup d ON b.post_id = d.dup_id
)
UPDATE post_bookmarks b
SET post_id = r.keep_id
FROM ranked r
WHERE b.id = r.id
  AND r.rn = 1
  AND NOT EXISTS (
      SELECT 1 FROM post_bookmarks k WHERE k.post_id = r.keep_id AND k.user_id = b.user_id
  );

DELETE FROM post_bookmarks WHERE post_id IN (SELECT dup_id FROM post_url_dedup);

-- 조회 기록/읽음 기록: 유니크 제약이 없으므로 그대로 옮기고 view_count 합산
UPDATE posts p
SET view_count = p.view_count + s.views
FROM (
    SELECT d.keep_id, SUM(dp.view_count) AS views
    FROM post_url_dedup d
    JOIN posts dp ON dp.id = d.dup_id
    GROUP BY d.keep_id
) s
WHERE p.id = s.keep_id;

UPDATE post_view v
SET post_id = d.keep_id
FROM post_url_dedup d
WHERE v.post_id = d.dup_id;

UPDATE anonymous_user_read_history h
SET post_id = d.keep_id
FROM post_url_dedup d
WHERE h.post_id = d.dup_id;

-- 요약/임베딩 결과물: 남기는 글의 상태와 일관되도록 중복 글의 것은 삭제
DELETE FROM post_categories       WHERE post_id IN (SELECT dup_id FROM post_url_dedup);
DELETE FROM post_embedding        WHERE post_id IN (SELECT dup_id FROM post_url_dedup);
DELETE FROM post_summary_failures WHERE post_id IN (SELECT dup_id FROM post_url_dedup);
DELETE FROM summary_retry_queue   WHERE post_id IN (SELECT dup_id FROM post_url_dedup);

DELETE FROM posts WHERE id IN (SELECT dup_id FROM post_url_dedup);

CREATE UNIQUE INDEX uq_posts_url ON posts (url);
