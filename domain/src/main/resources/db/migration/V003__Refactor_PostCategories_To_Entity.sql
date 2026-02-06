-- V003: Migrate post_categories from ElementCollection join table to standalone entity
-- Adds id column (BIGINT PK) to existing post_categories table

-- Step 1: Add id column
ALTER TABLE post_categories ADD COLUMN id BIGINT;

-- Step 2: Populate id values for existing rows
CREATE SEQUENCE post_categories_id_seq START WITH 1 INCREMENT BY 1;
UPDATE post_categories SET id = nextval('post_categories_id_seq');
DROP SEQUENCE post_categories_id_seq;

-- Step 3: Set NOT NULL and PRIMARY KEY
ALTER TABLE post_categories ALTER COLUMN id SET NOT NULL;
ALTER TABLE post_categories ADD CONSTRAINT pk_post_categories PRIMARY KEY (id);

-- Step 4: Add unique constraint to prevent duplicate categories per post
ALTER TABLE post_categories ADD CONSTRAINT uk_post_categories_post_category
    UNIQUE (post_id, category);

-- Step 5: Ensure index on post_id for foreign key lookups
CREATE INDEX IF NOT EXISTS idx_post_categories_post_id ON post_categories (post_id);
