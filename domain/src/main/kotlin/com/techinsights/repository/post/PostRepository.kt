package com.techinsights.repository.post

import com.techinsights.dto.post.PostDto

interface PostRepository {
  fun saveAll(posts: List<PostDto>): List<PostDto>
}