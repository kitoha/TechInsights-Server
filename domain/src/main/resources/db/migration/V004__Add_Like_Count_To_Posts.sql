-- ============================================================
-- Post 엔티티의 likeCount 필드에 대응하는 컬럼 추가
-- ============================================================
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS like_count BIGINT NOT NULL DEFAULT 0;
