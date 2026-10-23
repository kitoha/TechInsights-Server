-- ------------------------------------------------------------
-- 익명 좋아요 식별자를 IP 에서 aid 쿠키로 변경
-- 기존 IP 기반 행은 aid 로 매핑할 수 없으므로 anonymous_id = NULL 로 유지 (좋아요 수 보존)
-- ------------------------------------------------------------
ALTER TABLE post_likes ADD COLUMN IF NOT EXISTS anonymous_id VARCHAR(255);

DROP INDEX IF EXISTS uk_post_likes_anon;

-- 익명 사용자: 같은 aid 로 같은 게시글 중복 좋아요 방지
CREATE UNIQUE INDEX IF NOT EXISTS uk_post_likes_anon_aid
    ON post_likes (post_id, anonymous_id) WHERE user_id IS NULL AND anonymous_id IS NOT NULL;
