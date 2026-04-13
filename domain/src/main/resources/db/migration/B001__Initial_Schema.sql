-- pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================
-- company
-- ============================================================
CREATE TABLE IF NOT EXISTS company (
    id               BIGINT       NOT NULL,
    company_name     VARCHAR(255) NOT NULL,
    blog_url         VARCHAR(255) NOT NULL,
    logo_image_name  VARCHAR(255) NOT NULL,
    rss_supported    BOOLEAN      NOT NULL DEFAULT false,
    total_view_count BIGINT       NOT NULL DEFAULT 0,
    post_count       BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,
    deleted_at       TIMESTAMP,
    CONSTRAINT company_pkey             PRIMARY KEY (id),
    CONSTRAINT company_company_name_key UNIQUE (company_name)
);
CREATE INDEX IF NOT EXISTS idx_company_deleted_at ON company (deleted_at);

-- ============================================================
-- posts
-- ============================================================
CREATE TABLE IF NOT EXISTS posts (
    id                    BIGINT        NOT NULL,
    title                 VARCHAR(255)  NOT NULL,
    url                   VARCHAR(2048) NOT NULL,
    content               TEXT          NOT NULL,
    published_at          TIMESTAMP     NOT NULL,
    thumbnail             VARCHAR(2048),
    company_id            BIGINT        NOT NULL,
    view_count            BIGINT        NOT NULL DEFAULT 0,
    is_summary            BOOLEAN       NOT NULL DEFAULT false,
    preview               TEXT,
    is_embedding          BOOLEAN       NOT NULL DEFAULT false,
    summary_failure_count INT           NOT NULL DEFAULT 0,
    created_at            TIMESTAMP     NOT NULL,
    updated_at            TIMESTAMP     NOT NULL,
    deleted_at            TIMESTAMP,
    CONSTRAINT posts_pkey            PRIMARY KEY (id),
    CONSTRAINT posts_company_id_fkey FOREIGN KEY (company_id) REFERENCES company (id)
);
CREATE INDEX IF NOT EXISTS idx_posts_deleted_at    ON posts (deleted_at);
CREATE INDEX IF NOT EXISTS idx_posts_summary_retry ON posts (is_summary, summary_failure_count)
    WHERE is_summary = false;

-- ============================================================
-- post_categories
-- ============================================================
CREATE TABLE IF NOT EXISTS post_categories (
    post_id  BIGINT      NOT NULL,
    category VARCHAR(64) NOT NULL,
    CONSTRAINT post_categories_pkey         PRIMARY KEY (post_id, category),
    CONSTRAINT post_categories_post_id_fkey FOREIGN KEY (post_id) REFERENCES posts (id)
);
CREATE INDEX IF NOT EXISTS idx_post_categories_category ON post_categories (category);

-- ============================================================
-- post_embedding (pgvector)
-- ============================================================
CREATE TABLE IF NOT EXISTS post_embedding (
    post_id          BIGINT       NOT NULL,
    company_name     VARCHAR(255) NOT NULL,
    categories       VARCHAR(255) NOT NULL,
    content          TEXT         NOT NULL,
    embedding_vector vector(3072) NOT NULL,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,
    deleted_at       TIMESTAMP,
    CONSTRAINT post_embedding_pkey PRIMARY KEY (post_id)
);

-- ============================================================
-- post_summary_failures
-- ============================================================
CREATE TABLE IF NOT EXISTS post_summary_failures (
    id               BIGSERIAL   NOT NULL,
    post_id          BIGINT      NOT NULL,
    error_type       VARCHAR(50) NOT NULL,
    error_message    TEXT,
    failed_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    batch_size       INT,
    is_batch_failure BOOLEAN     NOT NULL DEFAULT false,
    CONSTRAINT post_summary_failures_pkey          PRIMARY KEY (id),
    CONSTRAINT fk_post_summary_failures_post_id    FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_psf_post_id    ON post_summary_failures (post_id);
CREATE INDEX IF NOT EXISTS idx_psf_error_type ON post_summary_failures (error_type);
CREATE INDEX IF NOT EXISTS idx_psf_failed_at  ON post_summary_failures (failed_at DESC);

-- ============================================================
-- post_view
-- ============================================================
CREATE TABLE IF NOT EXISTS post_view (
    id          BIGINT      NOT NULL,
    post_id     BIGINT      NOT NULL,
    user_or_ip  VARCHAR(64) NOT NULL,
    viewed_date TIMESTAMP   NOT NULL,
    user_agent  VARCHAR(512),
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL,
    deleted_at  TIMESTAMP,
    CONSTRAINT post_view_pkey         PRIMARY KEY (id),
    CONSTRAINT post_view_post_id_fkey FOREIGN KEY (post_id) REFERENCES posts (id)
);

-- ============================================================
-- users
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id               BIGINT      NOT NULL,
    email            VARCHAR(255) NOT NULL,
    name             VARCHAR(255) NOT NULL,
    nickname         VARCHAR(50),
    provider         VARCHAR(20) NOT NULL DEFAULT 'GOOGLE',
    provider_id      VARCHAR(255) NOT NULL,
    role             VARCHAR(20) NOT NULL DEFAULT 'USER',
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    profile_image    TEXT,
    marketing_agreed BOOLEAN     NOT NULL DEFAULT false,
    last_login_at    TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMPTZ,
    CONSTRAINT users_pkey       PRIMARY KEY (id),
    CONSTRAINT users_email_key  UNIQUE (email)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_provider_id ON users (provider, provider_id);
CREATE INDEX IF NOT EXISTS        idx_users_email        ON users (email);

-- ============================================================
-- refresh_tokens
-- ============================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id                  BIGINT      NOT NULL,
    user_id             BIGINT      NOT NULL,
    token_hash          TEXT        NOT NULL,
    previous_token_hash TEXT,
    device_id           TEXT,
    expiry_at           TIMESTAMPTZ NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMPTZ,
    CONSTRAINT refresh_tokens_pkey    PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_rt_hash      ON refresh_tokens (token_hash);
CREATE INDEX IF NOT EXISTS        idx_rt_prev_hash ON refresh_tokens (previous_token_hash);

-- ============================================================
-- anonymous_user_read_history
-- ============================================================
CREATE TABLE IF NOT EXISTS anonymous_user_read_history (
    id           BIGSERIAL    NOT NULL,
    anonymous_id VARCHAR(255) NOT NULL,
    post_id      BIGINT       NOT NULL,
    read_at      TIMESTAMP    NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL,
    deleted_at   TIMESTAMP,
    CONSTRAINT anonymous_user_read_history_pkey PRIMARY KEY (id)
);

-- ============================================================
-- Spring Batch 테이블
-- ============================================================
CREATE SEQUENCE IF NOT EXISTS batch_job_seq        INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS batch_job_execution_seq   INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS batch_step_execution_seq  INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 NO CYCLE;

CREATE TABLE IF NOT EXISTS batch_job_instance (
    job_instance_id BIGINT       NOT NULL DEFAULT nextval('batch_job_seq'),
    version         BIGINT,
    job_name        VARCHAR(100) NOT NULL,
    job_key         VARCHAR(32)  NOT NULL,
    CONSTRAINT batch_job_instance_pkey               PRIMARY KEY (job_instance_id),
    CONSTRAINT batch_job_instance_job_name_job_key_key UNIQUE (job_name, job_key)
);

CREATE TABLE IF NOT EXISTS batch_job_execution (
    job_execution_id BIGINT        NOT NULL DEFAULT nextval('batch_job_execution_seq'),
    version          BIGINT,
    job_instance_id  BIGINT        NOT NULL,
    create_time      TIMESTAMP     NOT NULL,
    start_time       TIMESTAMP,
    end_time         TIMESTAMP,
    status           VARCHAR(10),
    exit_code        VARCHAR(2500),
    exit_message     VARCHAR(2500),
    last_updated     TIMESTAMP,
    CONSTRAINT batch_job_execution_pkey                PRIMARY KEY (job_execution_id),
    CONSTRAINT batch_job_execution_job_instance_id_fkey FOREIGN KEY (job_instance_id) REFERENCES batch_job_instance (job_instance_id)
);

CREATE TABLE IF NOT EXISTS batch_job_execution_params (
    job_execution_id BIGINT        NOT NULL,
    parameter_name   VARCHAR(100)  NOT NULL,
    parameter_type   VARCHAR(100)  NOT NULL,
    parameter_value  VARCHAR(2500),
    identifying      CHAR(1)       NOT NULL,
    CONSTRAINT batch_job_execution_params_job_execution_id_fkey FOREIGN KEY (job_execution_id) REFERENCES batch_job_execution (job_execution_id)
);

CREATE TABLE IF NOT EXISTS batch_job_execution_context (
    job_execution_id   BIGINT        NOT NULL,
    short_context      VARCHAR(2500) NOT NULL,
    serialized_context TEXT,
    CONSTRAINT batch_job_execution_context_pkey                    PRIMARY KEY (job_execution_id),
    CONSTRAINT batch_job_execution_context_job_execution_id_fkey   FOREIGN KEY (job_execution_id) REFERENCES batch_job_execution (job_execution_id)
);

CREATE TABLE IF NOT EXISTS batch_step_execution (
    step_execution_id  BIGINT        NOT NULL DEFAULT nextval('batch_step_execution_seq'),
    version            BIGINT        NOT NULL,
    step_name          VARCHAR(100)  NOT NULL,
    job_execution_id   BIGINT        NOT NULL,
    create_time        TIMESTAMP     NOT NULL,
    start_time         TIMESTAMP,
    end_time           TIMESTAMP,
    status             VARCHAR(10),
    commit_count       BIGINT,
    read_count         BIGINT,
    filter_count       BIGINT,
    write_count        BIGINT,
    read_skip_count    BIGINT,
    write_skip_count   BIGINT,
    process_skip_count BIGINT,
    rollback_count     BIGINT,
    exit_code          VARCHAR(2500),
    exit_message       VARCHAR(2500),
    last_updated       TIMESTAMP,
    CONSTRAINT batch_step_execution_pkey                    PRIMARY KEY (step_execution_id),
    CONSTRAINT batch_step_execution_job_execution_id_fkey   FOREIGN KEY (job_execution_id) REFERENCES batch_job_execution (job_execution_id)
);

CREATE TABLE IF NOT EXISTS batch_step_execution_context (
    step_execution_id  BIGINT        NOT NULL,
    short_context      VARCHAR(2500) NOT NULL,
    serialized_context TEXT,
    CONSTRAINT batch_step_execution_context_pkey                       PRIMARY KEY (step_execution_id),
    CONSTRAINT batch_step_execution_context_step_execution_id_fkey     FOREIGN KEY (step_execution_id) REFERENCES batch_step_execution (step_execution_id)
);
