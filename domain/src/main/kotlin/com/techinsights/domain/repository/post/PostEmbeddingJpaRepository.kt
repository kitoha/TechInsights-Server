package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.PostEmbedding
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PostEmbeddingJpaRepository : JpaRepository<PostEmbedding, Long> {

  @Query(
    value = """
    SELECT post_id       AS postId,
           company_name  AS companyName,
           categories,
           content,
           embedding_vector <-> CAST(:targetVector AS vector) AS distance
    FROM post_embedding
    ORDER BY distance
    LIMIT :limit
    """,
    nativeQuery = true
  )
  fun findSimilarPostsAll(targetVector: String, limit: Long): List<PostEmbeddingWithDistance>

  @Query(
    value = """
    SELECT post_id       AS postId,
           company_name  AS companyName,
           categories,
           content,
           embedding_vector <-> CAST(:targetVector AS vector) AS distance
    FROM post_embedding
    WHERE post_id NOT IN :excludeIds
    ORDER BY distance
    LIMIT :limit
    """,
    nativeQuery = true
  )
  fun findSimilarPostsExcluding(targetVector: String, excludeIds: List<Long>, limit: Long): List<PostEmbeddingWithDistance>
}
