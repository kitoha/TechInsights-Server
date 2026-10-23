package com.techinsights.domain.repository.user

import com.techinsights.domain.dto.user.UserReadHistoryDto
import com.techinsights.domain.entity.user.UserReadHistory
import com.techinsights.domain.utils.Tsid
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class UserReadHistoryRepositoryImpl(
    private val jpaRepository: UserReadHistoryJpaRepository
) : UserReadHistoryRepository {

  override fun trackUserPostRead(userId: Long, postId: String) {
    val history = UserReadHistory(
      id = Tsid.generateLong(),
      userId = userId,
      postId = Tsid.decode(postId)
    )
    jpaRepository.save(history)
  }

  override fun getRecentReadHistory(
    userId: Long,
    pageable: Pageable
  ): List<UserReadHistoryDto> {
    return jpaRepository.findByUserIdOrderByReadAtDesc(userId, pageable)
      .map { UserReadHistoryDto.fromEntity(it) }
  }
}
