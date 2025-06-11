package com.techinsights.entity.company

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
  val rssSupported: Boolean = false
)