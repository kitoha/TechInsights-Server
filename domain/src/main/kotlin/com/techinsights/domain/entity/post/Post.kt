package com.techinsights.domain.entity.post

import com.techinsights.domain.entity.BaseEntity
import com.techinsights.domain.entity.company.Company
import com.techinsights.domain.enums.Category
import com.techinsights.domain.utils.Tsid
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
  @OneToMany(
    mappedBy = "post",
    cascade = [CascadeType.ALL],
    orphanRemoval = true,
    fetch = FetchType.LAZY
  )
  @BatchSize(size = 100)
  var postCategories: MutableSet<PostCategory> = mutableSetOf(),
  @Column(name = "is_summary", nullable = false)
  var isSummary: Boolean = false,
  @Column(name = "is_embedding", nullable = false)
  var isEmbedding: Boolean = false,
  @Column(name = "summary_failure_count", nullable = false)
  var summaryFailureCount: Int = 0,
  @Column(name = "like_count", nullable = false)
  var likeCount: Long = 0L
) : BaseEntity() {

  val categoryValues: Set<Category>
    get() = postCategories.map { it.category }.toSet()

  fun updateCategories(categories: Set<Category>) {
    postCategories.removeAll { it.category !in categories }

    val existingCategories = postCategories.map { it.category }.toSet()
    categories
      .filter { it !in existingCategories }
      .forEach { category ->
        postCategories.add(
          PostCategory(
            id = Tsid.generateLong(),
            post = this,
            category = category
          )
        )
      }
  }
}