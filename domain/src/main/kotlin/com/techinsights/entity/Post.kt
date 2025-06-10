package com.techinsights.entity

import jakarta.persistence.Column
import jakarta.persistence.Id
import java.time.LocalDateTime
import jakarta.persistence.Entity
import jakarta.persistence.Table

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
  @Column(name = "categories")
  var thumbnail: String? = null,
) {
}