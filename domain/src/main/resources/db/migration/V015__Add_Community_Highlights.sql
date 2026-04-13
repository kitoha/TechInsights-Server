-- community_highlights 컬럼 추가
ALTER TABLE github_repository_community
    ADD COLUMN IF NOT EXISTS community_highlights JSONB;
