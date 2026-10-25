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
    // Same property (and default) as the api's refresh-token lifetime: a retired hash must outlive the longest-lived
    // refresh JWT that could still present it, or a valid-but-retired token would be NotFound instead of reuse.
    @Value("\${auth.jwt.refresh-token-expiration:30d}")
    private val refreshTokenLifetime: Duration
) : Tasklet {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val now = Instant.now()
        val expiredSessions = refreshTokenService.deleteExpired(now)
        val clearedSuccessors = refreshTokenService.clearExpiredSuccessors(now)
        val oldRetiredHashes = refreshTokenService.deleteRetiredBefore(now.minus(refreshTokenLifetime))
        log.info(
            "Refresh token cleanup: {} expired sessions deleted, {} sealed successors cleared, " +
                "{} retired hashes older than {} deleted",
            expiredSessions, clearedSuccessors, oldRetiredHashes, refreshTokenLifetime
        )
        return RepeatStatus.FINISHED
    }
}
