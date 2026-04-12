-- ============================================================
-- github_repository_readme
-- ============================================================
CREATE TABLE IF NOT EXISTS github_repository_readme (
    repo_id                   BIGINT        NOT NULL,
    readme_summary            TEXT,
    readme_summarized_at      TIMESTAMP,
    readme_summary_error_type VARCHAR(50),
    readme_embedded_at        TIMESTAMP,
    readme_embedding_vector   vector(3072),
    created_at                TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_github_repository_readme PRIMARY KEY (repo_id),
    CONSTRAINT fk_github_repository_readme FOREIGN KEY (repo_id)
        REFERENCES github_repositories(id)
);

CREATE INDEX idx_readme_hnsw
    ON github_repository_readme
    USING hnsw ((readme_embedding_vector::halfvec(3072)) halfvec_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- ============================================================
-- github_repository_community
-- 커뮤니티 집계 상태 / 카운트
-- ============================================================
CREATE TABLE IF NOT EXISTS github_repository_community (
    repo_id                     BIGINT      NOT NULL,
    community_status            VARCHAR(20),
    community_collected_at      TIMESTAMP,
    community_fetched_at        TIMESTAMP,
    community_raw_mention_count INTEGER     NOT NULL DEFAULT 0,
    community_mention_count     INTEGER     NOT NULL DEFAULT 0,
    sentiment_positive          INTEGER     NOT NULL DEFAULT 0,
    sentiment_neutral           INTEGER     NOT NULL DEFAULT 0,
    sentiment_negative          INTEGER     NOT NULL DEFAULT 0,
    community_insights          JSONB,
    community_error_type        VARCHAR(50),
    community_update_count      INTEGER     NOT NULL DEFAULT 0,
    created_at                  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_github_repository_community PRIMARY KEY (repo_id),
    CONSTRAINT fk_github_repository_community FOREIGN KEY (repo_id)
        REFERENCES github_repositories(id)
);

-- Collect job 커서 인덱스 (오래된 것부터 수집)
CREATE INDEX idx_grc_collect
    ON github_repository_community (community_collected_at ASC NULLS FIRST, repo_id ASC);

-- Analyze job: PENDING 상태만 빠르게 조회
CREATE INDEX idx_grc_analyze_pending
    ON github_repository_community (repo_id)
    WHERE community_status = 'PENDING';

-- 에러 재시도 인덱스
CREATE INDEX idx_grc_retry
    ON github_repository_community (community_error_type, community_fetched_at)
    WHERE community_error_type IS NOT NULL;

-- ============================================================
-- community_posts (1:N)
-- 개별 게시글 + per-post 감정 라벨
-- ============================================================
CREATE TABLE IF NOT EXISTS community_posts (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    repo_id               BIGINT       NOT NULL,
    platform              VARCHAR(20)  NOT NULL,
    url                   TEXT         NOT NULL,
    title                 TEXT         NOT NULL,
    score                 INTEGER      NOT NULL DEFAULT 0,
    comment_count         INTEGER      NOT NULL DEFAULT 0,
    post_created_at       TIMESTAMP,
    sentiment             VARCHAR(20),
    sentiment_analyzed_at TIMESTAMP,
    collected_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_community_posts_repo FOREIGN KEY (repo_id)
        REFERENCES github_repositories(id)
);

-- 중복 수집 방지
CREATE UNIQUE INDEX uq_community_posts_repo_url
    ON community_posts (repo_id, url);

-- Analyze job: 미분석 게시글 조회
CREATE INDEX idx_community_posts_unanalyzed
    ON community_posts (repo_id)
    WHERE sentiment IS NULL;
