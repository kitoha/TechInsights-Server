package com.techinsights.batch.maintenance.config

import com.techinsights.batch.common.listener.LoggingJobExecutionListener
import com.techinsights.batch.maintenance.tasklet.RefreshTokenCleanupTasklet
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class RefreshTokenCleanupJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val refreshTokenCleanupTasklet: RefreshTokenCleanupTasklet,
    private val loggingJobExecutionListener: LoggingJobExecutionListener
) {

    @Bean
    fun refreshTokenCleanupJob(): Job = JobBuilder("refreshTokenCleanupJob", jobRepository)
        .incrementer(RunIdIncrementer())
        .listener(loggingJobExecutionListener)
        .start(refreshTokenCleanupStep())
        .build()

    @Bean
    fun refreshTokenCleanupStep(): Step =
        StepBuilder("refreshTokenCleanupStep", jobRepository)
            .tasklet(refreshTokenCleanupTasklet, transactionManager)
            .build()
}
