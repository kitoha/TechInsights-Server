package com.techinsights.domain.service.post

import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.utils.Tsid
import org.springframework.stereotype.Service

@Service
class RdbViewCountUpdater (
  private val postRepository: PostRepository
) : ViewCountUpdater {

  override fun incrementViewCount(postId: String) {
    postRepository.incrementViewCount(Tsid.decode(postId))
  }
}