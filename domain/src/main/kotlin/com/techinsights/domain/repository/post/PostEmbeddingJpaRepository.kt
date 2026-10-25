package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.PostEmbedding
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PostEmbeddingJpaRepository : JpaRepository<PostEmbedding, Long> {

  @Query(
    value = """
    SELECT pe.post_id       AS postId,
           pe.company_name  AS companyName,
           pe.categories,
           pe.content,
           pe.embedding_vector::halfvec(3072) <=> CAST(:targetVector AS halfvec) AS distance
    FROM post_embedding pe
    JOIN posts p ON p.id = pe.post_id AND p.deleted_at IS NULL
    JOIN company c ON c.id = p.company_id AND c.deleted_at IS NULL
    WHERE pe.deleted_at IS NULL
    ORDER BY distance
    LIMIT :limit
    """,
    nativeQuery = true
  )
  fun findSimilarPostsAll(targetVector: String, limit: Long): List<PostEmbeddingWithDistance>

  @Query(
    value = """
    SELECT e.post_id       AS postId,
           e.company_name  AS companyName,
           e.categories,
           e.content,
           e.embedding_vector::halfvec(3072) <=> CAST(:targetVector AS halfvec) AS distance
    FROM post_embedding e
    JOIN posts p ON p.id = e.post_id AND p.deleted_at IS NULL
    JOIN company c ON c.id = p.company_id AND c.deleted_at IS NULL
    WHERE e.deleted_at IS NULL
      AND p.company_id = :companyId
    ORDER BY distance
    LIMIT :limit
    """,
    nativeQuery = true
  )
  fun findSimilarPostsByCompany(targetVector: String, companyId: Long, limit: Long): List<PostEmbeddingWithDistance>

  @Query(
    value = """
    SELECT pe.post_id       AS postId,
           pe.company_name  AS companyName,
           pe.categories,
           pe.content,
           pe.embedding_vector::halfvec(3072) <=> CAST(:targetVector AS halfvec) AS distance
    FROM post_embedding pe
    JOIN posts p ON p.id = pe.post_id AND p.deleted_at IS NULL
    JOIN company c ON c.id = p.company_id AND c.deleted_at IS NULL
    WHERE pe.deleted_at IS NULL
      AND pe.post_id NOT IN :excludeIds
    ORDER BY distance
    LIMIT :limit
    """,
    nativeQuery = true
  )
  fun findSimilarPostsExcluding(targetVector: String, excludeIds: List<Long>, limit: Long): List<PostEmbeddingWithDistance>
}
