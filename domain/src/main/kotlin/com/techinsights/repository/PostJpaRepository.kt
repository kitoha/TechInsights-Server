package com.techinsights.repository

import com.techinsights.entity.Post
import org.springframework.data.jpa.repository.JpaRepository

interface PostJpaRepository : JpaRepository<Post, Long> {
}