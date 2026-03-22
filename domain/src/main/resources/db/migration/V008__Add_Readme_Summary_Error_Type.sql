-- readme 요약 실패 시 오류 유형을 저장하는 컬럼 추가
ALTER TABLE github_repositories
    ADD COLUMN readme_summary_error_type VARCHAR(50) NULL;

-- 재시도 후보 쿼리를 위한 인덱스
CREATE INDEX idx_github_repos_retry_candidates
    ON github_repositories (readme_summary_error_type, readme_summarized_at)
    WHERE readme_summary_error_type IS NOT NULL
      AND deleted_at IS NULL;
