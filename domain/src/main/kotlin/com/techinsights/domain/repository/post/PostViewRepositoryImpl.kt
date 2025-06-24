package com.techinsights.domain.repository.post

import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.post.PostViewDto
import com.techinsights.domain.entity.post.QPostView
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class PostViewRepositoryImpl(
  private val jpaQueryFactory: JPAQueryFactory,
  private val postViewJpaRepository: PostViewJpaRepository
): PostViewRepository {

  override fun save(postViewDto: PostViewDto): PostViewDto {
    val postView = postViewDto.toEntity()
    val savedPostView = postViewJpaRepository.save(postView)
    return PostViewDto.fromEntity(savedPostView)
  }

  override fun existsByPostIdAndUserOrIpAndViewedDate(
    postId: Long, userOrIp: String, viewedDate: LocalDate
  ): Boolean {
    val postView = QPostView.postView

    return jpaQueryFactory.selectFrom(postView)
      .where(
        postView.postId.eq(postId),
        postView.userOrIp.eq(userOrIp),
        postView.viewedDate.eq(viewedDate)
      )
      .fetchFirst() != null
  }

}