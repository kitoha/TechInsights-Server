ALTER TABLE github_repositories
    ADD COLUMN readme_embedding_vector vector(3072),
    ADD COLUMN readme_embedded_at      TIMESTAMP;
