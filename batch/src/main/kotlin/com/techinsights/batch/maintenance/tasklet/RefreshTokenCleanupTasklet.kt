package com.techinsights.batch.maintenance.tasklet

import com.techinsights.domain.service.user.RefreshTokenService
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class RefreshTokenCleanupTasklet(
    private val refreshTokenService: RefreshTokenService,
    @Value("\${batch.refresh-token-cleanup.retired-hash-retention:30d}")
    private val retiredHashRetention: Duration
) : Tasklet {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val now = Instant.now()
        val expiredSessions = refreshTokenService.deleteExpired(now)
        val oldRetiredHashes = refreshTokenService.deleteRetiredBefore(now.minus(retiredHashRetention))
        log.info(
            "Refresh token cleanup: {} expired sessions deleted, {} retired hashes older than {} deleted",
            expiredSessions, oldRetiredHashes, retiredHashRetention
        )
        return RepeatStatus.FINISHED
    }
}
