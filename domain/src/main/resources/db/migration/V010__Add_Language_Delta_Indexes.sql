-- 주간 트렌딩 복합 인덱스
CREATE INDEX idx_github_repositories_language_weekly_delta
    ON github_repositories (primary_language, weekly_star_delta DESC)
    WHERE deleted_at IS NULL;

-- 언어 필터 + 일간 트렌딩 복합 인덱스
CREATE INDEX idx_github_repositories_language_daily_delta
    ON github_repositories (primary_language, daily_star_delta DESC)
    WHERE deleted_at IS NULL;
