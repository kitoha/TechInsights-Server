package com.techinsights.batch.summary.tasklet

import com.techinsights.domain.service.company.CompanyViewCountUpdater
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.batch.repeat.RepeatStatus

class CompanyPostCountReconcileTaskletTest : FunSpec({

    val companyViewCountUpdater = mockk<CompanyViewCountUpdater>()
    val tasklet = CompanyPostCountReconcileTasklet(companyViewCountUpdater)

    test("execute should reconcile company post counts and finish") {
        every { companyViewCountUpdater.reconcilePostCounts() } returns 3

        val status = tasklet.execute(mockk(relaxed = true), mockk(relaxed = true))

        status shouldBe RepeatStatus.FINISHED
        verify(exactly = 1) { companyViewCountUpdater.reconcilePostCounts() }
    }
})
