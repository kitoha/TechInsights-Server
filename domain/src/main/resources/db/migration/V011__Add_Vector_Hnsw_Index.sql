-- readme_embedding_vector 컬럼에 HNSW 인덱스 추가
-- halfvec 캐스팅 인덱스 사용
CREATE INDEX IF NOT EXISTS idx_github_repositories_readme_embedding_hnsw
    ON github_repositories
    USING hnsw ((readme_embedding_vector::halfvec(3072)) halfvec_cosine_ops)
    WITH (m = 16, ef_construction = 64)
    WHERE deleted_at IS NULL
      AND readme_embedding_vector IS NOT NULL;
