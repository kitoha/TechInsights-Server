-- ------------------------------------------------------------
-- 로그인 사용자의 읽기 이력 (추천용).
-- 익명 이력(anonymous_user_read_history)과 같은 모양이지만 user_id 에 FK 를 두어
-- 사용자 삭제 시 함께 지워지도록 별도 테이블로 둔다.
-- ------------------------------------------------------------
CREATE TABLE user_read_history (
    id         BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,
    post_id    BIGINT    NOT NULL,
    read_at    TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    CONSTRAINT user_read_history_pkey PRIMARY KEY (id),
    CONSTRAINT fk_user_read_history_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 사용자별 최근 읽은 글 조회 (anonymous 쪽 idx_anon_read_history_anonymous_read_at 과 같은 모양)
CREATE INDEX idx_user_read_history_user_read_at ON user_read_history (user_id, read_at DESC);
