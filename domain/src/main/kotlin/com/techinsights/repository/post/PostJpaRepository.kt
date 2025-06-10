package com.techinsights.repository.post

import com.techinsights.entity.post.Post
import org.springframework.data.jpa.repository.JpaRepository

interface PostJpaRepository : JpaRepository<Post, Long> {
}