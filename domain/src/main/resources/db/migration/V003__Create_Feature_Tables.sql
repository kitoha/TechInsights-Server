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
    CONSTRAINT pk_post_likes          PRIMARY KEY (id),
    CONSTRAINT uk_post_likes_post_user UNIQUE (post_id, user_id)
);

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
