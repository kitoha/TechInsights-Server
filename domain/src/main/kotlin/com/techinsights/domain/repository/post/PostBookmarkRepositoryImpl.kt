package com.techinsights.domain.repository.post

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.entity.post.PostBookmark
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class PostBookmarkRepositoryImpl(
    private val postBookmarkJpaRepository: PostBookmarkJpaRepository,
) : PostBookmarkRepository {

    override fun saveAndFlush(bookmark: PostBookmark): PostBookmark =
        postBookmarkJpaRepository.saveAndFlush(bookmark)

    override fun findByPostIdAndUserId(postId: Long, userId: Long): PostBookmark? =
        postBookmarkJpaRepository.findByPostIdAndUserId(postId, userId)

    override fun deleteByPostIdAndUserId(postId: Long, userId: Long): Long =
        postBookmarkJpaRepository.deleteByPostIdAndUserId(postId, userId)

    override fun findBookmarkedPosts(userId: Long, pageable: Pageable): Page<PostDto> =
        postBookmarkJpaRepository.findBookmarkedPosts(userId, pageable).map { PostDto.fromEntity(it) }
}
