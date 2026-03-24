-- daily trending 지원을 위한 컬럼 추가
ALTER TABLE github_repositories
    ADD COLUMN daily_star_delta               BIGINT    NOT NULL DEFAULT 0,
    ADD COLUMN star_count_prev_day            BIGINT,
    ADD COLUMN star_count_prev_day_updated_at TIMESTAMP;

-- daily trending 정렬 인덱스
CREATE INDEX idx_github_repositories_daily_delta
    ON github_repositories (daily_star_delta DESC)
    WHERE deleted_at IS NULL;
