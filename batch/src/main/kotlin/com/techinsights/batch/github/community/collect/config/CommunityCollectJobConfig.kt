package com.techinsights.batch.github.community.collect.config

import com.techinsights.batch.common.listener.LoggingJobExecutionListener
import com.techinsights.batch.github.community.collect.config.props.CommunityCollectBatchProperties
import com.techinsights.batch.github.community.collect.processor.CommunityCollectProcessor
import com.techinsights.batch.github.community.collect.reader.CommunityCollectReader
import com.techinsights.batch.github.community.collect.writer.CommunityCollectWriter
import com.techinsights.batch.github.community.dto.CommunityBuzzInput
import com.techinsights.domain.dto.github.GithubRepositoryDto
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
@EnableConfigurationProperties(CommunityCollectBatchProperties::class)
class CommunityCollectJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val properties: CommunityCollectBatchProperties,
    private val communityCollectReader: CommunityCollectReader,
    private val communityCollectProcessor: CommunityCollectProcessor,
    private val communityCollectWriter: CommunityCollectWriter,
    private val loggingJobExecutionListener: LoggingJobExecutionListener,
) {

    @Bean
    fun communityCollectJob(
        @Qualifier("communityCollectStep") step: Step,
    ): Job = JobBuilder(properties.jobName, jobRepository)
        .incrementer(RunIdIncrementer())
        .listener(loggingJobExecutionListener)
        .start(step)
        .build()

    @Bean("communityCollectStep")
    fun communityCollectStep(): Step =
        StepBuilder(properties.stepName, jobRepository)
            .chunk<GithubRepositoryDto, CommunityBuzzInput>(properties.chunkSize, transactionManager)
            .reader(communityCollectReader)
            .processor(communityCollectProcessor)
            .writer(communityCollectWriter)
            .build()
}
