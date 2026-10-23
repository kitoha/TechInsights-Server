-- ------------------------------------------------------------
-- 리프레시 토큰 패밀리: 로그인 1회 = 세션(패밀리) 1개.
-- 회전으로 폐기된 토큰의 해시를 패밀리별로 보관해, 한 단계 이전이 아닌
-- 더 오래된 토큰의 재사용도 감지하고 해당 패밀리(세션)만 폐기한다.
-- ------------------------------------------------------------

ALTER TABLE refresh_tokens ADD COLUMN family_id BIGINT;

-- 기존 세션은 각자 하나의 패밀리로 취급
UPDATE refresh_tokens SET family_id = id;

ALTER TABLE refresh_tokens ALTER COLUMN family_id SET NOT NULL;
ALTER TABLE refresh_tokens ADD CONSTRAINT uk_rt_family UNIQUE (family_id);

CREATE TABLE refresh_token_retired_hashes (
    token_hash TEXT        NOT NULL,
    family_id  BIGINT      NOT NULL,
    retired_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT refresh_token_retired_hashes_pkey PRIMARY KEY (token_hash),
    CONSTRAINT fk_rt_retired_family FOREIGN KEY (family_id)
        REFERENCES refresh_tokens (family_id) ON DELETE CASCADE
);
CREATE INDEX idx_rt_retired_family ON refresh_token_retired_hashes (family_id);
CREATE INDEX idx_rt_retired_at     ON refresh_token_retired_hashes (retired_at);

-- 기존 previous_token_hash 를 폐기 해시 테이블로 이관
INSERT INTO refresh_token_retired_hashes (token_hash, family_id, retired_at)
SELECT previous_token_hash, family_id, updated_at
FROM refresh_tokens
WHERE previous_token_hash IS NOT NULL
ON CONFLICT (token_hash) DO NOTHING;

DROP INDEX IF EXISTS idx_rt_prev_hash;
ALTER TABLE refresh_tokens DROP COLUMN previous_token_hash;

-- 만료 정리 / 사용자별 최근 사용 순 조회(세션 상한)
CREATE INDEX idx_rt_expiry       ON refresh_tokens (expiry_at);
CREATE INDEX idx_rt_user_updated ON refresh_tokens (user_id, updated_at DESC);
