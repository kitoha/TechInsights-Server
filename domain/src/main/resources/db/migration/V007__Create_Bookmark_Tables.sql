CREATE TABLE IF NOT EXISTS post_bookmarks (
    id         BIGINT    NOT NULL,
    post_id    BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    CONSTRAINT pk_post_bookmarks      PRIMARY KEY (id),
    CONSTRAINT uq_post_bookmark       UNIQUE (post_id, user_id),
    CONSTRAINT fk_post_bookmarks_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_bookmarks_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_post_bookmarks_user_id
    ON post_bookmarks (user_id);

CREATE TABLE IF NOT EXISTS github_bookmarks (
    id         BIGINT    NOT NULL,
    repo_id    BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    CONSTRAINT pk_github_bookmarks      PRIMARY KEY (id),
    CONSTRAINT uq_github_bookmark       UNIQUE (repo_id, user_id),
    CONSTRAINT fk_github_bookmarks_repo FOREIGN KEY (repo_id) REFERENCES github_repositories (id) ON DELETE CASCADE,
    CONSTRAINT fk_github_bookmarks_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_github_bookmarks_user_id
    ON github_bookmarks (user_id);
