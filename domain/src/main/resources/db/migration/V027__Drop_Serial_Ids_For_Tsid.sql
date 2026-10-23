-- ------------------------------------------------------------
-- AGENTS.md: ID 는 자동 증가가 아닌 Tsid(애플리케이션 할당). 기존 id 값은 그대로 유지
-- ------------------------------------------------------------
ALTER TABLE anonymous_user_read_history ALTER COLUMN id DROP DEFAULT;
ALTER TABLE post_summary_failures       ALTER COLUMN id DROP DEFAULT;

DROP SEQUENCE IF EXISTS anonymous_user_read_history_id_seq;
DROP SEQUENCE IF EXISTS post_summary_failures_id_seq;
