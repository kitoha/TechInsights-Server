ALTER TABLE github_repositories
    ADD COLUMN readme_embedding_vector vector(3072),
    ADD COLUMN readme_embedded_at      TIMESTAMP;

CREATE INDEX idx_github_repositories_embedding_vector
    ON github_repositories
    USING hnsw (readme_embedding_vector vector_l2_ops)
    WHERE deleted_at IS NULL
      AND readme_embedding_vector IS NOT NULL;
