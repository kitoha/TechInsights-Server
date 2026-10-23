-- ------------------------------------------------------------
-- V018/V022 가 post_view 의 중복 행을 지웠지만 posts.view_count 에는 그 조회가 남아 있음.
-- post_view 는 완전한 이력이다: 조회수 증가는 항상 post_view 행 저장 이후에만 발행되고,
-- post_view 행을 삭제/보관 정리하는 코드는 없다. 그래서 행 수로 한 번 재계산한다.
-- ------------------------------------------------------------
UPDATE posts p
SET view_count = COALESCE(v.cnt, 0)
FROM posts target
LEFT JOIN (
    SELECT post_id, COUNT(*) AS cnt
    FROM post_view
    GROUP BY post_id
) v ON v.post_id = target.id
WHERE p.id = target.id
  AND p.view_count <> COALESCE(v.cnt, 0);

-- 회사 총 조회수는 소속 게시글 조회수의 합 (조회마다 게시글/회사 카운터가 함께 증가)
UPDATE company c
SET total_view_count = COALESCE(s.total, 0)
FROM company target
LEFT JOIN (
    SELECT company_id, SUM(view_count) AS total
    FROM posts
    GROUP BY company_id
) s ON s.company_id = target.id
WHERE c.id = target.id
  AND c.total_view_count <> COALESCE(s.total, 0);
