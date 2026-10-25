-- ------------------------------------------------------------
-- 닉네임 유일성을 DB가 보장 (대소문자 무시, 삭제되지 않은 사용자 기준)
-- ------------------------------------------------------------

-- Hibernate 가 Flyway 이전에 만들었을 수 있는 users(nickname) 단일 컬럼 UNIQUE 제약/인덱스 제거.
-- 대소문자를 구분하고 삭제된 사용자까지 포함하므로 닉네임 재사용을 막고 500 으로 노출된다. 없으면 아무 것도 하지 않는다.
DO $$
DECLARE
    nickname_attnum smallint;
    r record;
BEGIN
    SELECT attnum INTO nickname_attnum
    FROM pg_attribute
    WHERE attrelid = 'users'::regclass AND attname = 'nickname' AND NOT attisdropped;

    FOR r IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'users'::regclass
          AND contype = 'u'
          AND conkey = ARRAY[nickname_attnum]
    LOOP
        EXECUTE format('ALTER TABLE users DROP CONSTRAINT %I', r.conname);
    END LOOP;

    FOR r IN
        SELECT c.relname AS index_name
        FROM pg_index i
        JOIN pg_class c ON c.oid = i.indexrelid
        WHERE i.indrelid = 'users'::regclass
          AND i.indisunique
          AND NOT i.indisprimary
          AND i.indnatts = 1
          AND i.indkey[0] = nickname_attnum
          AND i.indexprs IS NULL
          AND i.indpred IS NULL
    LOOP
        EXECUTE format('DROP INDEX %I', r.index_name);
    END LOOP;
END $$;

-- 활성 사용자 중 lower(nickname) 이 같은 그룹: created_at(동률이면 id)이 가장 빠른 사용자가 유지하고,
-- 나머지는 'User_' || id 끝 12자리(<= 17자, NicknameValidator 최대 20자 이내)로 바꾼다.
-- 이미 쓰이는 이름과 겹치면(끝자리 충돌 포함) md5 기반 12자리로 바꿔가며 비어 있는 이름을 찾는다.
-- 한 명씩 순서대로 갱신하므로 앞서 바뀐 이름과도 충돌하지 않는다.
DO $$
DECLARE
    r record;
    candidate text;
    attempt int;
BEGIN
    FOR r IN
        SELECT id
        FROM (
            SELECT id,
                   ROW_NUMBER() OVER (PARTITION BY lower(nickname) ORDER BY created_at, id) AS rn
            FROM users
            WHERE deleted_at IS NULL
              AND nickname IS NOT NULL
        ) ranked
        WHERE rn > 1
        ORDER BY id
    LOOP
        attempt := 0;
        candidate := 'User_' || right(r.id::text, 12);
        WHILE EXISTS (
            SELECT 1 FROM users
            WHERE deleted_at IS NULL AND lower(nickname) = lower(candidate) AND id <> r.id
        ) LOOP
            attempt := attempt + 1;
            candidate := 'User_' || substr(md5(r.id::text || ':' || attempt), 1, 12);
        END LOOP;

        UPDATE users SET nickname = candidate WHERE id = r.id;
    END LOOP;
END $$;

CREATE UNIQUE INDEX uk_users_nickname_lower
    ON users (lower(nickname))
    WHERE deleted_at IS NULL;
