package com.techinsights.domain.repository.post

import com.techinsights.domain.dto.post.PostDto

interface PostRepository {
  fun saveAll(posts: List<PostDto>): List<PostDto>
}