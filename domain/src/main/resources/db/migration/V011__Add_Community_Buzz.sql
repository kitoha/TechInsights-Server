ALTER TABLE github_repositories
    ADD COLUMN community_insights      JSONB,
    ADD COLUMN community_highlights    JSONB,
    ADD COLUMN community_sentiment     JSONB,
    ADD COLUMN community_mention_count INTEGER,
    ADD COLUMN community_status        VARCHAR(20),
    ADD COLUMN community_fetched_at    TIMESTAMP,
    ADD COLUMN community_error_type    VARCHAR(50),
    ADD COLUMN community_update_count  INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_github_repos_community_candidates
    ON github_repositories (community_fetched_at ASC NULLS FIRST, id ASC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_github_repos_community_retry
    ON github_repositories (community_error_type, community_fetched_at)
    WHERE community_error_type IS NOT NULL
      AND deleted_at IS NULL;
