package com.techinsights.batch.summary.tasklet

import com.techinsights.domain.service.company.CompanyViewCountUpdater
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component

@Component
class CompanyPostCountReconcileTasklet(
    private val companyViewCountUpdater: CompanyViewCountUpdater
) : Tasklet {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val corrected = companyViewCountUpdater.reconcilePostCounts()
        log.info("Reconciled company post_count: {} companies corrected", corrected)
        return RepeatStatus.FINISHED
    }
}
