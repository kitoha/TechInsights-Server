package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.PostEmbedding
import org.springframework.data.jpa.repository.JpaRepository

interface PostEmbeddingRepository : JpaRepository<PostEmbedding, Long>
