package com.techinsights.domain.event

import com.techinsights.domain.service.company.CompanyViewCountUpdater
import com.techinsights.domain.service.post.ViewCountUpdater
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ViewCountEventHandler(
  private val viewCountUpdater: ViewCountUpdater,
  private val companyViewCountUpdater: CompanyViewCountUpdater
) {

  companion object {

    private val logger = KotlinLogging.logger {}
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun handleViewCountIncrement(event: ViewCountIncrementEvent) {
    try {
      viewCountUpdater.incrementViewCount(event.postId)
      companyViewCountUpdater.incrementTotalViewCount(event.companyId)

      logger.debug { "View count incremented for postId: ${event.postId}, companyId: ${event.companyId}" }
    } catch (e: Exception) {
      logger.error(e) { "Failed to increment view count for postId: ${event.postId}, companyId: ${event.companyId}" }
    }
  }
}
