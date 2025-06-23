package com.techinsights.domain.service.post

import com.techinsights.domain.entity.post.PostView
import com.techinsights.domain.repository.post.PostViewRepository
import com.techinsights.domain.utils.Tsid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PostViewService(
  private val postViewRepository: PostViewRepository,
  private val viewCountUpdater: ViewCountUpdater
) {

  @Transactional
  fun recordView(postId: String, userOrIp: String) {
    val today = LocalDateTime.now()

    if (!postViewRepository.existsByPostIdAndUserOrIpAndViewedDate(
        Tsid.decode(postId), userOrIp, today
      )
    ) {
      val postView = PostView(
        id = Tsid.decode(Tsid.generate()), postId = Tsid.decode(postId),
        userOrIp = userOrIp, viewedDate = today
      )
      postViewRepository.save(postView)

      viewCountUpdater.incrementViewCount(postId)
    }
  }
}