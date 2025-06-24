package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.PostView
import org.springframework.data.jpa.repository.JpaRepository

interface PostViewJpaRepository : JpaRepository<PostView, Long> {

}