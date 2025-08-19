package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.PostEmbedding
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PostEmbeddingJpaRepository : JpaRepository<PostEmbedding, Long>{
  @Query(
    value = """
    SELECT *
    FROM post_embedding
    WHERE post_id NOT IN :excludeIds
    ORDER BY embedding_vector <-> CAST(:targetVector AS vector)
    LIMIT :limit
  """,
    nativeQuery = true
  )
  fun findSimilarPosts(targetVector: String, excludeIds: List<Long>, limit: Int): List<PostEmbedding>
}
