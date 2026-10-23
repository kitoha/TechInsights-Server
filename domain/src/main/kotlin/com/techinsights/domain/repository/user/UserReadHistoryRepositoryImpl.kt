package com.techinsights.domain.repository.user

import com.techinsights.domain.dto.user.UserReadHistoryDto
import com.techinsights.domain.utils.Tsid
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
class UserReadHistoryRepositoryImpl(
    private val jpaRepository: UserReadHistoryJpaRepository
) : UserReadHistoryRepository {

  @Transactional
  override fun trackUserPostRead(userId: Long, postId: String) {
    jpaRepository.upsertRead(
      id = Tsid.generateLong(),
      userId = userId,
      postId = Tsid.decode(postId),
      readAt = LocalDateTime.now()
    )
  }

  override fun getRecentReadHistory(
    userId: Long,
    pageable: Pageable
  ): List<UserReadHistoryDto> {
    return jpaRepository.findByUserIdOrderByReadAtDesc(userId, pageable)
      .map { UserReadHistoryDto.fromEntity(it) }
  }
}
