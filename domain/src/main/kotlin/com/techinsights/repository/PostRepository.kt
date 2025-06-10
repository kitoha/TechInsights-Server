package com.techinsights.repository

import com.techinsights.dto.PostDto
import com.techinsights.entity.Post

interface PostRepository {
  fun saveAll(posts: List<PostDto>): List<PostDto>
}