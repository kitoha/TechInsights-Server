-- ------------------------------------------------------------
-- 닉네임 유일성을 DB가 보장 (대소문자 무시, 삭제되지 않은 사용자 기준)
-- ------------------------------------------------------------

-- 활성 사용자 중 lower(nickname) 이 같은 그룹: created_at(동률이면 id)이 가장 빠른 사용자가 유지하고,
-- 나머지는 'User_' || id 로 변경한다.
UPDATE users u
SET nickname = 'User_' || u.id
FROM (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY lower(nickname) ORDER BY created_at, id) AS rn
    FROM users
    WHERE deleted_at IS NULL
      AND nickname IS NOT NULL
) ranked
WHERE u.id = ranked.id
  AND ranked.rn > 1;

CREATE UNIQUE INDEX uk_users_nickname_lower
    ON users (lower(nickname))
    WHERE deleted_at IS NULL;
