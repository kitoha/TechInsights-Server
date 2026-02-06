package com.techinsights.domain.repository.post

import com.techinsights.domain.entity.post.PostLike
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class PostLikeRepositoryImpl(
    private val postLikeJpaRepository: PostLikeJpaRepository
) : PostLikeRepository {
    override fun save(postLike: PostLike): PostLike {
        return postLikeJpaRepository.save(postLike)
    }

    override fun findByPostIdAndUserId(postId: Long, userId: Long): PostLike? {
        return postLikeJpaRepository.findByPostIdAndUserId(postId, userId)
    }

    override fun findByPostIdAndIpAddress(postId: Long, ipAddress: String): PostLike? {
        return postLikeJpaRepository.findByPostIdAndIpAddress(postId, ipAddress)
    }

    override fun findAnonymousByPostIdAndIpAddress(postId: Long, ipAddress: String): PostLike? {
        return postLikeJpaRepository.findByPostIdAndIpAddressAndUserIdIsNull(postId, ipAddress)
    }

    override fun deleteByPostIdAndUserId(postId: Long, userId: Long): Long {
        return postLikeJpaRepository.deleteByPostIdAndUserId(postId, userId)
    }

    override fun deleteByPostIdAndIpAddress(postId: Long, ipAddress: String): Long {
        return postLikeJpaRepository.deleteByPostIdAndIpAddress(postId, ipAddress)
    }

    override fun deleteAnonymousByPostIdAndIpAddress(postId: Long, ipAddress: String): Long {
        return postLikeJpaRepository.deleteByPostIdAndIpAddressAndUserIdIsNull(postId, ipAddress)
    }

    override fun findAllByUserId(userId: Long, pageable: Pageable): Page<PostLike> {
        return postLikeJpaRepository.findAllByUserId(userId, pageable)
    }

    override fun findAllByIpAddress(ipAddress: String, pageable: Pageable): Page<PostLike> {
        return postLikeJpaRepository.findAllByIpAddress(ipAddress, pageable)
    }
}
