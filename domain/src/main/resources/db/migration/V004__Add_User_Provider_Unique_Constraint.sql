DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_users_provider_provider_id'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT uk_users_provider_provider_id
                UNIQUE (provider, provider_id);
    END IF;
END $$;
