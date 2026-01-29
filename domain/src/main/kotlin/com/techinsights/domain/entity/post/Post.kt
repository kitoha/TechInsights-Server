package com.techinsights.domain.entity.post

import com.techinsights.domain.entity.BaseEntity
import com.techinsights.domain.entity.company.Company
import com.techinsights.domain.enums.Category
import jakarta.persistence.*
import org.hibernate.annotations.BatchSize
import java.time.LocalDateTime

@Entity
@Table(name = "posts")
class Post(
  @Id
  val id: Long,
  @Column(name = "title", length = 500)
  val title: String,
  @Column(name = "preview", columnDefinition = "TEXT")
  var preview: String?,
  @Column(name = "url", length = 1000)
  val url: String,
  @Column(name = "content")
  val content: String,
  @Column(name = "published_at")
  val publishedAt: LocalDateTime,
  @Column(name = "thumbnail", length = 1000)
  var thumbnail: String? = null,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", nullable = false)
  val company: Company,
  @Column(name = "view_count", nullable = false)
  var viewCount: Long = 0L,
  @BatchSize(size = 100)
  @ElementCollection(fetch = FetchType.LAZY, targetClass = Category::class)
  @CollectionTable(name = "post_categories", joinColumns = [JoinColumn(name = "post_id")])
  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false)
  var categories: MutableSet<Category> = mutableSetOf(),
  @Column(name = "is_summary", nullable = false)
  var isSummary: Boolean = false,
  @Column(name = "is_embedding", nullable = false)
  var isEmbedding: Boolean = false,
  @Column(name = "summary_failure_count", nullable = false)
  var summaryFailureCount: Int = 0
) : BaseEntity()