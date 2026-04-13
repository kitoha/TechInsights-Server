ALTER TABLE github_repository_community
    ADD COLUMN IF NOT EXISTS community_sentiment JSONB;
