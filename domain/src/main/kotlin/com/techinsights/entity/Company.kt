package com.techinsights.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "company")
class Company(
  @Id
  val id : Long,
  @Column(name = "company_name")
  val name: String,
  @Column(name = "blog_url")
  val blogUrl: String,
  @Column(name = "rss_supported")
  val rssSupported: Boolean = false,
)