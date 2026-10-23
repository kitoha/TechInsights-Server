-- ------------------------------------------------------------
-- 인기순 게시글 목록 (ORDER BY view_count DESC, id DESC LIMIT n) 용 인덱스
-- idx_posts_summary_cursor 와 같은 조건(is_summary = true)의 부분 인덱스
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_posts_summary_popular
    ON posts (view_count DESC, id DESC)
    WHERE is_summary = true;
