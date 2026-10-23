-- ------------------------------------------------------------
-- 엔티티 매핑과 어긋난 컬럼 정렬 (hibernate ddl-auto=validate 로 검출)
-- ------------------------------------------------------------

-- Post.title 은 length = 500: 255자를 넘는 제목 때문에 크롤링 청크 전체가 실패하던 문제
ALTER TABLE posts ALTER COLUMN title TYPE VARCHAR(500);

-- PostView.viewedDate 는 LocalDate(일 단위). 같은 (게시글, 식별자, 날짜) 중복은 가장 오래된 행만 유지
DELETE FROM post_view a
    USING post_view b
WHERE a.post_id     = b.post_id
  AND a.user_or_ip  = b.user_or_ip
  AND a.viewed_date::date = b.viewed_date::date
  AND (a.created_at, a.id) > (b.created_at, b.id);

ALTER TABLE post_view ALTER COLUMN viewed_date TYPE DATE USING viewed_date::date;
