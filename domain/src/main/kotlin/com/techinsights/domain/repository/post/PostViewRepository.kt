package com.techinsights.domain.repository.post

import com.techinsights.domain.dto.post.PostViewDto

interface PostViewRepository {
  fun save(postViewDto: PostViewDto): PostViewDto

  /** Inserts the view unless the same (post, identifier, date) already exists. Returns true if a row was inserted. */
  fun insertIfAbsent(postViewDto: PostViewDto): Boolean
}
