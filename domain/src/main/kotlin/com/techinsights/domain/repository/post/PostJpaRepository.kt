package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.Post
import org.springframework.data.jpa.repository.JpaRepository

interface PostJpaRepository : JpaRepository<Post, Long> {
}