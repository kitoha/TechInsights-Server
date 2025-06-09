package com.techinsights.dto

data class Post(
  val id: String,
  val title: String,
  val url: String,
  val content: String,
  val publishedAt: String,
  val categories: List<String>,
  val company: String? = null,
  val thumbnailUrl: String? = null,
  val viewCount : Int? = null,
  val likeCount : Int? = null,
)
