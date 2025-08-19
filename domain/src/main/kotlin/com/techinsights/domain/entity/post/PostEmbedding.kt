package com.techinsights.domain.entity.post

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "post_embedding")
class PostEmbedding(
    @Id
    @Column(name = "post_id")
    val postId: Long,

    @Column(name = "company_name", nullable = false)
    val companyName: String,

    @Column(name = "categories", nullable = false)
    val categories: String,

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    val content: String,

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "embedding_vector", columnDefinition = "vector(768)")
    val embeddingVector: FloatArray
)
