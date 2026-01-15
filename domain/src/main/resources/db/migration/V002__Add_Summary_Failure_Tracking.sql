-- Add failure count to posts table
ALTER TABLE posts
ADD COLUMN summary_failure_count INT NOT NULL DEFAULT 0;

-- Create index for failure filtering
CREATE INDEX idx_posts_summary_retry ON posts(is_summary, summary_failure_count)
WHERE is_summary = false;

-- Create failure history table
CREATE TABLE post_summary_failures (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    error_type VARCHAR(50) NOT NULL,
    error_message TEXT,
    failed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    batch_size INT,
    is_batch_failure BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_post_summary_failures_post_id FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

-- Create indexes for failure history
CREATE INDEX idx_psf_post_id ON post_summary_failures(post_id);
CREATE INDEX idx_psf_error_type ON post_summary_failures(error_type);
CREATE INDEX idx_psf_failed_at ON post_summary_failures(failed_at DESC);

-- Comment for documentation
COMMENT ON COLUMN posts.summary_failure_count IS 'Number of times summary generation has failed for this post';
COMMENT ON TABLE post_summary_failures IS 'Detailed history of summary generation failures for monitoring and debugging';
