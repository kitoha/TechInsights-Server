package com.techinsights.domain.service.post

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.dto.post.PostViewDto
import com.techinsights.domain.repository.post.PostViewRepository
import com.techinsights.domain.service.company.CompanyViewCountUpdater
import com.techinsights.domain.utils.Tsid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class PostViewService(
  private val postViewRepository: PostViewRepository,
  private val viewCountUpdater: ViewCountUpdater,
  private val companyViewCountUpdater: CompanyViewCountUpdater
) {

  @Transactional
  fun recordView(post : PostDto, userOrIp: String) {
    val viewDate = LocalDate.now()
    val today = LocalDateTime.now() // 후에 auditing 필요시 변경
    val postId = post.id
    val companyId = post.company.id

    if (!postViewRepository.existsByPostIdAndUserOrIpAndViewedDate(
        Tsid.decode(postId), userOrIp, viewDate
      )
    ) {
      val postView = PostViewDto(
        id = Tsid.generate(), postId = postId,
        userOrIp = userOrIp, viewedDate = viewDate, createdAt = today
      )
      postViewRepository.save(postView)

      viewCountUpdater.incrementViewCount(postId)
      companyViewCountUpdater.incrementTotalViewCount(companyId)
    }
  }
}