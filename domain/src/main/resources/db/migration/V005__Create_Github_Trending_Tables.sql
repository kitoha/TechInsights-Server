CREATE TABLE IF NOT EXISTS github_repositories (
    id                              BIGINT        NOT NULL,
    repo_name                       VARCHAR(255)  NOT NULL,
    full_name                       VARCHAR(500)  NOT NULL,
    description                     TEXT,
    html_url                        VARCHAR(1000) NOT NULL,
    star_count                      BIGINT        NOT NULL DEFAULT 0,
    fork_count                      BIGINT        NOT NULL DEFAULT 0,
    primary_language                VARCHAR(100),
    owner_name                      VARCHAR(255)  NOT NULL,
    owner_avatar_url                VARCHAR(1000),
    topics                          TEXT,
    pushed_at                       TIMESTAMP     NOT NULL,
    fetched_at                      TIMESTAMP     NOT NULL,
    weekly_star_delta               BIGINT        NOT NULL DEFAULT 0,
    star_count_prev_week            BIGINT,
    star_count_prev_week_updated_at TIMESTAMP,
    readme_summary                  TEXT,
    readme_summarized_at            TIMESTAMP,
    created_at                      TIMESTAMP     NOT NULL,
    updated_at                      TIMESTAMP     NOT NULL,
    deleted_at                      TIMESTAMP,
    CONSTRAINT pk_github_repositories PRIMARY KEY (id),
    CONSTRAINT uq_github_repositories_full_name UNIQUE (full_name)
);

CREATE INDEX idx_github_repositories_star_count
    ON github_repositories (star_count DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_github_repositories_pushed_at
    ON github_repositories (pushed_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_github_repositories_weekly_delta
    ON github_repositories (weekly_star_delta DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_github_repositories_language_star
    ON github_repositories (primary_language, star_count DESC)
    WHERE deleted_at IS NULL;
