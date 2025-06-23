package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.PostView
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface PostViewRepository : JpaRepository<PostView, Long> {
  fun existsByPostIdAndUserOrIpAndViewedDate(postId: Long, userOrIp: String, viewedDate: LocalDateTime): Boolean

}