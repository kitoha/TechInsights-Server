-- readme_embedding_vector 컬럼에 HNSW 인덱스 추가
-- 개선: HNSW 인덱스를 통한 ANN(Approximate Nearest Neighbor) 검색
-- m = 16            : 각 레이어의 최대 연결 수
-- ef_construction = 64 : 인덱스 빌드 시 탐색 범위
-- vector_cosine_ops : 코사인 거리 연산자
CREATE INDEX IF NOT EXISTS idx_github_repositories_readme_embedding_hnsw
    ON github_repositories
    USING hnsw (readme_embedding_vector vector_cosine_ops)
    WITH (m = 16, ef_construction = 64)
    WHERE deleted_at IS NULL
      AND readme_embedding_vector IS NOT NULL;
