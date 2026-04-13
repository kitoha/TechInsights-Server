-- README 컬럼 제거 (github_repository_readme 테이블로 이전 완료)
ALTER TABLE github_repositories
    DROP COLUMN IF EXISTS readme_summary,
    DROP COLUMN IF EXISTS readme_summarized_at,
    DROP COLUMN IF EXISTS readme_summary_error_type,
    DROP COLUMN IF EXISTS readme_embedded_at,
    DROP COLUMN IF EXISTS readme_embedding_vector;

-- 커뮤니티 컬럼 제거 (github_repository_community / community_posts 로 이전 완료)
ALTER TABLE github_repositories
    DROP COLUMN IF EXISTS community_status,
    DROP COLUMN IF EXISTS community_collected_at,
    DROP COLUMN IF EXISTS community_fetched_at,
    DROP COLUMN IF EXISTS community_raw_mention_count,
    DROP COLUMN IF EXISTS community_mention_count,
    DROP COLUMN IF EXISTS community_highlights,
    DROP COLUMN IF EXISTS community_sentiment,
    DROP COLUMN IF EXISTS community_insights,
    DROP COLUMN IF EXISTS community_error_type,
    DROP COLUMN IF EXISTS community_update_count;

-- HNSW 인덱스는 github_repository_readme로 이전했으므로 기존 인덱스 제거
DROP INDEX IF EXISTS idx_github_repositories_readme_embedding_hnsw;
