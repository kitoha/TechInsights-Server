-- ------------------------------------------------------------
-- [1] cursor-based pagination 인덱스
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_posts_not_summary_cursor
    ON posts (is_summary, published_at, id)
    WHERE is_summary = false;

CREATE INDEX IF NOT EXISTS idx_posts_summary_not_embedded_cursor
    ON posts (is_summary, is_embedding, published_at, id)
    WHERE is_summary = true AND is_embedding = false;

CREATE INDEX IF NOT EXISTS idx_posts_summary_cursor
    ON posts (is_summary, published_at, id)
    WHERE is_summary = true;

-- ------------------------------------------------------------
-- [2] post_categories: 복합 PK → id 단일 PK 전환 (구 V003 미적용)
-- ------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM   information_schema.columns
        WHERE  table_schema = 'public'
          AND  table_name   = 'post_categories'
          AND  column_name  = 'id'
    ) THEN

        ALTER TABLE post_categories ADD COLUMN id BIGINT;

        CREATE SEQUENCE temp_pc_id_seq START WITH 1 INCREMENT BY 1;
        UPDATE post_categories SET id = nextval('temp_pc_id_seq');
        DROP SEQUENCE temp_pc_id_seq;

        ALTER TABLE post_categories ALTER COLUMN id SET NOT NULL;
        ALTER TABLE post_categories DROP CONSTRAINT post_categories_pkey;
        ALTER TABLE post_categories ADD CONSTRAINT pk_post_categories PRIMARY KEY (id);

        ALTER TABLE post_categories
            ADD CONSTRAINT uk_post_categories_post_category UNIQUE (post_id, category);

        CREATE INDEX idx_post_categories_post_id ON post_categories (post_id);
    END IF;
END $$;

-- ------------------------------------------------------------
-- [3] users: named unique constraint 추가 후 중복 index 정리
-- ------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_users_provider_provider_id'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT uk_users_provider_provider_id UNIQUE (provider, provider_id);
    END IF;
END $$;

-- constraint 생성으로 동일한 unique index가 생성되므로 기존 중복 index 제거
DROP INDEX IF EXISTS idx_users_provider_id;

-- ------------------------------------------------------------
-- [4] refresh_tokens: 누락 인덱스 추가
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_rt_user_device ON refresh_tokens (user_id, device_id);
