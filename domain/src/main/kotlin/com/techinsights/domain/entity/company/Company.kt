package com.techinsights.domain.entity.company

import com.techinsights.domain.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "company", uniqueConstraints = [UniqueConstraint(columnNames = ["name"])])
class Company(
  @Id
  val id : Long,
  @Column(name = "company_name")
  val name: String,
  @Column(name = "blog_url")
  val blogUrl: String,
  @Column(name = "logo_image_name")
  val logoImageName: String,
  @Column(name = "rss_supported")
  val rssSupported: Boolean = false,
  @Column(name = "total_view_count", nullable = false)
  var totalViewCount: Long = 0L,
  @Column(name = "post_count", nullable = false)
  var postCount: Long = 0L
) : BaseEntity()