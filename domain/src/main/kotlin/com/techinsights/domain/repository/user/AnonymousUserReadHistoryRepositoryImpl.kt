package com.techinsights.domain.repository.user

import com.techinsights.domain.entity.user.AnonymousUserReadHistory
import com.techinsights.domain.utils.Tsid
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class AnonymousUserReadHistoryRepositoryImpl(
    private val jpaRepository: AnonymousUserReadHistoryJpaRepository
) : AnonymousUserReadHistoryRepository {

  override fun trackAnonymousPostRead(anonymousId: String, postId: String) {
    val postIdLong = Tsid.decode(postId)
    val history = AnonymousUserReadHistory(anonymousId = anonymousId, postId = postIdLong)
    jpaRepository.save(history)
  }

  override fun getRecentReadHistory(anonymousId: String, pageable: Pageable): List<AnonymousUserReadHistory> {
    return jpaRepository.findTopNByAnonymousIdOrderByReadAtDesc(anonymousId, pageable)
  }
}