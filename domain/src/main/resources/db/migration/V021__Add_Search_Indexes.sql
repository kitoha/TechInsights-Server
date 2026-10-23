-- 게시글 시맨틱 검색: halfvec 캐스팅 HNSW 인덱스 (V011, V013과 동일한 표현식)
CREATE INDEX IF NOT EXISTS idx_post_embedding_hnsw
    ON post_embedding
    USING hnsw ((embedding_vector::halfvec(3072)) halfvec_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 키워드 검색(LIKE '%q%'): pg_trgm GIN 인덱스. 대소문자 무시 조건은 lower(...) 표현식과 맞춘다.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_posts_title_trgm
    ON posts USING gin (lower(title) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_posts_preview_trgm
    ON posts USING gin (lower(preview) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_posts_content_trgm
    ON posts USING gin (content gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_company_name_trgm
    ON company USING gin (lower(company_name) gin_trgm_ops);

-- 회사명 일치(company_id IN (...))를 posts의 BitmapOr 항으로 처리하기 위한 인덱스
CREATE INDEX IF NOT EXISTS idx_posts_company_id
    ON posts (company_id);
