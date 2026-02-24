-- ------------------------------------------------------------
-- post_likes (PostLike 엔티티)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS post_likes (
    id         BIGINT      NOT NULL,
    post_id    BIGINT      NOT NULL,
    user_id    BIGINT,
    ip_address VARCHAR(64) NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,
    deleted_at TIMESTAMP,
    CONSTRAINT pk_post_likes      PRIMARY KEY (id),
    CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 로그인 사용자: 같은 게시글에 중복 좋아요 방지
-- (UNIQUE (post_id, user_id) 는 user_id=NULL 인 행을 모두 통과시켜 무효 → 부분 인덱스 사용)
CREATE UNIQUE INDEX IF NOT EXISTS uk_post_likes_user
    ON post_likes (post_id, user_id) WHERE user_id IS NOT NULL;

-- 익명 사용자: 같은 IP로 같은 게시글 중복 좋아요 방지
CREATE UNIQUE INDEX IF NOT EXISTS uk_post_likes_anon
    ON post_likes (post_id, ip_address) WHERE user_id IS NULL;

-- ------------------------------------------------------------
-- summary_retry_queue (SummaryRetryQueue 엔티티)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS summary_retry_queue (
    post_id       BIGINT        NOT NULL,
    reason        VARCHAR(1000) NOT NULL,
    error_type    VARCHAR(50)   NOT NULL,
    retry_count   INT           NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ   NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    last_retry_at TIMESTAMPTZ,
    max_retries   INT           NOT NULL DEFAULT 5,
    CONSTRAINT pk_summary_retry_queue PRIMARY KEY (post_id)
);
CREATE INDEX IF NOT EXISTS idx_next_retry_at ON summary_retry_queue (next_retry_at);
CREATE INDEX IF NOT EXISTS idx_retry_count   ON summary_retry_queue (retry_count);
