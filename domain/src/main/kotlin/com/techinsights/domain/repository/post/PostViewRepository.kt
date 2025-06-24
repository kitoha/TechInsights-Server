package com.techinsights.domain.repository.post

import com.techinsights.domain.dto.post.PostViewDto
import java.time.LocalDate

interface PostViewRepository {
  fun save(postViewDto: PostViewDto): PostViewDto
  fun existsByPostIdAndUserOrIpAndViewedDate(postId: Long, userOrIp: String, viewedDate: LocalDate): Boolean
}