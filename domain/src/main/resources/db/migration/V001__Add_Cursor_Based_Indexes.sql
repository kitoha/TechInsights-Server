/**
  Fly Way will be applied
 */

CREATE INDEX IF NOT EXISTS idx_posts_not_summary_cursor
ON posts (is_summary, published_at, id)
WHERE is_summary = false;

CREATE INDEX IF NOT EXISTS idx_posts_summary_not_embedded_cursor
ON posts (is_summary, is_embedding, published_at, id)
WHERE is_summary = true AND is_embedding = false;

CREATE INDEX IF NOT EXISTS idx_posts_summary_cursor
ON posts (is_summary, published_at, id)
WHERE is_summary = true;

COMMENT ON INDEX idx_posts_not_summary_cursor IS
'Cursor-based pagination index for batch processing. Replaces offset-based queries with cursor conditions: WHERE published_at > ? OR (published_at = ? AND id > ?)';

COMMENT ON INDEX idx_posts_summary_not_embedded_cursor IS
'Cursor-based pagination index for embedding batch. Enables efficient cursor navigation through summarized posts awaiting embedding.';

COMMENT ON INDEX idx_posts_summary_cursor IS
'Cursor-based pagination index for summarized posts. General purpose index for cursor-based access to all summarized content.';
