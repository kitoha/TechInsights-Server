package com.techinsights.domain.service.post

import com.techinsights.domain.dto.post.PostViewDto
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.repository.post.PostViewRepository
import com.techinsights.domain.service.company.CompanyViewCountUpdater
import com.techinsights.domain.utils.Tsid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class PostViewService(
  private val postViewRepository: PostViewRepository,
  private val postRepository: PostRepository,
  private val viewCountUpdater: ViewCountUpdater,
  private val companyViewCountUpdater: CompanyViewCountUpdater
) {

  @Transactional
  fun recordView(postId: String, userOrIp: String, userAgent: String?) {
    val viewDate = LocalDate.now()
    val decodedPostId = Tsid.decode(postId)

    if (!postViewRepository.existsByPostIdAndUserOrIpAndViewedDate(
        decodedPostId, userOrIp, viewDate
      )
    ) {

      val companyId = postRepository.getCompanyIdByPostId(postId)

      val postView = PostViewDto(
        id = Tsid.generate(),
        postId = postId,
        userOrIp = userOrIp,
        viewedDate = viewDate,
        userAgent = userAgent
      )
      postViewRepository.save(postView)

      viewCountUpdater.incrementViewCount(postId)
      companyViewCountUpdater.incrementTotalViewCount(companyId)
    }
  }
}