package com.techinsights.domain.entity.github

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Array
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "github_repository_readme")
@EntityListeners(AuditingEntityListener::class)
class GithubRepositoryReadme(
    @Id
    val repoId: Long,

    @Column(name = "readme_summary", columnDefinition = "TEXT")
    var readmeSummary: String? = null,

    @Column(name = "readme_summarized_at")
    var readmeSummarizedAt: LocalDateTime? = null,

    @Column(name = "readme_summary_error_type", length = 50)
    var readmeSummaryErrorType: String? = null,

    @Column(name = "readme_embedded_at")
    var readmeEmbeddedAt: LocalDateTime? = null,

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 3072)
    @Column(name = "readme_embedding_vector")
    var readmeEmbeddingVector: FloatArray? = null,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null,
)
