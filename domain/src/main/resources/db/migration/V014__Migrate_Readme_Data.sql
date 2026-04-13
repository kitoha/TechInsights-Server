DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'github_repositories'
          AND column_name = 'readme_summary'
    ) THEN
        INSERT INTO github_repository_readme (
            repo_id,
            readme_summary,
            readme_summarized_at,
            readme_summary_error_type,
            readme_embedded_at,
            readme_embedding_vector,
            created_at,
            updated_at
        )
        SELECT
            id,
            readme_summary,
            readme_summarized_at,
            readme_summary_error_type,
            readme_embedded_at,
            readme_embedding_vector,
            created_at,
            updated_at
        FROM github_repositories
        WHERE readme_summary IS NOT NULL
           OR readme_summarized_at IS NOT NULL
           OR readme_embedding_vector IS NOT NULL
        ON CONFLICT (repo_id) DO NOTHING;
    END IF;
END $$;
