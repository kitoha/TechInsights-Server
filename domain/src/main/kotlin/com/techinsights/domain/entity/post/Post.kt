package com.techinsights.domain.entity.post

import com.techinsights.domain.entity.company.Company
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "posts")
class Post(
  @Id
  val id: Long,
  @Column(name = "title")
  val title: String,
  @Column(name = "url")
  val url: String,
  @Column(name = "content")
  val content: String,
  @Column(name = "published_at")
  val publishedAt: LocalDateTime,
  @Column(name = "thumbnail")
  var thumbnail: String? = null,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", nullable = false)
  val company: Company
) {
}