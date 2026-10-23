package com.techinsights.domain.service.post

import com.techinsights.domain.dto.post.PostViewDto
import com.techinsights.domain.event.ViewCountIncrementEvent
import com.techinsights.domain.exception.PostNotFoundException
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.repository.post.PostViewRepository
import com.techinsights.domain.repository.user.AnonymousUserReadHistoryRepository
import com.techinsights.domain.repository.user.UserReadHistoryRepository
import com.techinsights.domain.utils.Tsid
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class PostViewService(
  private val postViewRepository: PostViewRepository,
  private val postRepository: PostRepository,
  private val anonymousUserReadHistoryRepository: AnonymousUserReadHistoryRepository,
  private val userReadHistoryRepository: UserReadHistoryRepository,
  private val applicationEventPublisher: ApplicationEventPublisher
) {

  @Transactional
  fun recordView(postId: String, identifier: String, userAgent: String?) {
    requireExistingPost(postId)

    val postView = PostViewDto(
      id = Tsid.generate(),
      postId = postId,
      userOrIp = identifier,
      viewedDate = LocalDate.now(),
      userAgent = userAgent
    )

    if (postViewRepository.insertIfAbsent(postView)) {
      val companyId = postRepository.getCompanyIdByPostId(postId)

      applicationEventPublisher.publishEvent(
        ViewCountIncrementEvent(postId, companyId)
      )
    }
  }

  @Transactional
  fun trackAnonymousPostRead(anonymousId: String, postId: String) {
    anonymousUserReadHistoryRepository.trackAnonymousPostRead(anonymousId, postId)
  }

  @Transactional
  fun trackUserPostRead(userId: Long, postId: String) {
    requireExistingPost(postId)
    userReadHistoryRepository.trackUserPostRead(userId, postId)
  }

  private fun requireExistingPost(postId: String) {
    val id = runCatching { Tsid.decode(postId) }.getOrNull()
    if (id == null || !postRepository.existsById(id)) {
      throw PostNotFoundException("Post with ID $postId not found")
    }
  }
}
