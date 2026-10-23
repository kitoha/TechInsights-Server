-- ------------------------------------------------------------
-- 익명 사용자 식별자(aid 쿠키) 기준 조회 중복 제거 / 읽기 이력 조회 인덱스
-- ------------------------------------------------------------

-- post_view: 같은 (게시글, 식별자, 날짜) 중복 행 제거 (가장 오래된 행만 유지)
DELETE FROM post_view a
    USING post_view b
WHERE a.post_id     = b.post_id
  AND a.user_or_ip  = b.user_or_ip
  AND a.viewed_date = b.viewed_date
  AND a.id          > b.id;

-- INSERT ... ON CONFLICT DO NOTHING 으로 check-then-insert 경쟁 상태를 제거
ALTER TABLE post_view
    ADD CONSTRAINT uk_post_view_post_identifier_date UNIQUE (post_id, user_or_ip, viewed_date);

-- anonymous_user_read_history: 식별자별 최근 읽은 글 조회 (추천)
CREATE INDEX IF NOT EXISTS idx_anon_read_history_anonymous_read_at
    ON anonymous_user_read_history (anonymous_id, read_at DESC);
