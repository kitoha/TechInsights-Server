package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.Post
import com.techinsights.domain.entity.post.PostBookmark
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostBookmarkJpaRepository : JpaRepository<PostBookmark, Long> {

    fun findByPostIdAndUserId(postId: Long, userId: Long): PostBookmark?

    fun deleteByPostIdAndUserId(postId: Long, userId: Long): Long

    @Query(
        value = """
            SELECT p FROM Post p
            JOIN PostBookmark pb ON p.id = pb.postId
            WHERE pb.userId = :userId
            ORDER BY pb.createdAt DESC
        """,
        countQuery = """
            SELECT COUNT(p) FROM Post p
            JOIN PostBookmark pb ON p.id = pb.postId
            WHERE pb.userId = :userId
        """
    )
    fun findBookmarkedPosts(@Param("userId") userId: Long, pageable: Pageable): Page<Post>
}
