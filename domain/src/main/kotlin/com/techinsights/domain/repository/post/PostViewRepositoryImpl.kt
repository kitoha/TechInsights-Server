package com.techinsights.domain.repository.post

import com.techinsights.domain.dto.post.PostViewDto
import org.springframework.stereotype.Repository

@Repository
class PostViewRepositoryImpl(
  private val postViewJpaRepository: PostViewJpaRepository
): PostViewRepository {

  override fun save(postViewDto: PostViewDto): PostViewDto {
    val postView = postViewDto.toEntity()
    val savedPostView = postViewJpaRepository.save(postView)
    return PostViewDto.fromEntity(savedPostView)
  }

  override fun insertIfAbsent(postViewDto: PostViewDto): Boolean {
    val postView = postViewDto.toEntity()
    return postViewJpaRepository.insertIfAbsent(
      id = postView.id,
      postId = postView.postId,
      userOrIp = postView.userOrIp,
      viewedDate = postView.viewedDate,
      userAgent = postView.userAgent
    ) > 0
  }

}
